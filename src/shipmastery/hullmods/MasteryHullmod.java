package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import shipmastery.ShipMasteryNPC;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.VariantLookup;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.util.NavigableMap;
import java.util.Objects;

public class MasteryHullmod extends BaseHullMod implements HullModFleetEffect {

    private boolean noSync = false;

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    @Override
    public void onFleetSync(final CampaignFleetAPI fleet) {
        if (noSync || fleet == null) {
            return;
        }
        // Ignore NPC fleets that aren't interacting with the player
        if (!fleet.isPlayerFleet()) {
            if (fleet.getBattle() == null || !fleet.getBattle().isPlayerInvolved()) {
                return;
            }
        }
        // Make sure every ship in the fleet has this hullmod installed
        // Do it outside this call stack as changing the ship immediately actually causes some unexpected changes
        // i.e. the "you just purchased a ship" screen no longer appears as the purchased ship is
        // no longer the same as the one in the player's fleet
        DeferredActionPlugin.performLater(new Action() {
            @Override
            public void perform() {
                // Guaranteed not null
                PersonAPI commander = fleet.getCommander();
                for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
                    ShipVariantAPI variant = fm.getVariant();
                    fm.setVariant(addHandlerMod(variant, variant, commander), false, false);
                    // Add mastery indicator hullmods to NPC fleets
                    if (!fleet.isPlayerFleet()) {
                        NavigableMap<Integer, Boolean>
                                levels = ShipMasteryNPC.getActiveMasteriesForCommander(commander, fm.getHullSpec());
                        if (!levels.isEmpty()) {
                            int maxLevel = levels.lastEntry().getKey();
                            if (maxLevel >= 1) {
                                maxLevel = Math.min(maxLevel, 9);
                                fm.getVariant().addMod("sms_npcIndicator" + maxLevel);
                            }
                        }
                    }
                }

                if (fleet.isPlayerFleet()) {
                    for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
                        Utils.fixVariantInconsistencies(fm.getVariant());
                    }
                }
            }
        }, 0f);
    }

    /** Modifies and returns the given variant if it's not a stock, goal, or empty variant
     *  (those can be duplicated across multiple ships).
     *  Otherwise, returns a modified copy of that variant. */
    ShipVariantAPI addHandlerMod(ShipVariantAPI variant, ShipVariantAPI root, PersonAPI commander) {
        if (variant.isStockVariant() || variant.isGoalVariant() || variant.isEmptyHullVariant()) {
            variant = variant.clone();
            variant.setSource(VariantSource.REFIT);
        }
        VariantLookup.addVariantInfo(variant, root, commander);
        // Bypass the arbitrary checks in removeMod since we're adding it back anyway
        // Makes sure the mastery handler is the last hullmod processed (backing DS is LinkedHashSet)
        variant.getHullMods().remove("sms_masteryHandler");
        variant.getHullMods().add("sms_masteryHandler");
        // This also sets hasOpAffectingMods to null, forcing variants to
        // recompute their statsForOpCosts
        // (Normally this is naturally set when a hullmod is manually added or removed)
        variant.addPermaMod("sms_masteryHandler");
        // Add the tracker to any modules as well
        for (String id : variant.getModuleSlots()) {
            variant.setModuleVariant(id, addHandlerMod(variant.getModuleVariant(id), root, commander));
        }
        return variant;
    }

    @Override
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats,
                                               final String id) {
        FleetMemberAPI fm = stats.getFleetMember();
        final PersonAPI captain = fm == null ? null : fm.getCaptain();
        applyEffects(stats.getVariant(), new HullmodAction() {
            @Override
            public void perform(MasteryEffect effect, PersonAPI commander, boolean isModule) {
                if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                    effect.applyEffectsBeforeShipCreation(hullSize, stats);
                    // For display purposes only
                    if (commander != null && Objects.equals(commander, captain)) {
                        effect.onFlagshipStatusGained(commander, stats, null);
                    }
                }
            }
        });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        final PersonAPI captain = ship.getCaptain();
        applyEffects(ship.getVariant(), new HullmodAction() {
            @Override
            public void perform(MasteryEffect effect, PersonAPI commander, boolean isModule) {
                if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                    effect.applyEffectsAfterShipCreation(ship);
                    // For display purposes only
                    if (commander != null && Objects.equals(commander, captain)) {
                        effect.onFlagshipStatusGained(commander, ship.getMutableStats(), null);
                    }
                }
            }
        });
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        applyEffects(ship.getVariant(), new HullmodAction() {
            @Override
            public void perform(MasteryEffect effect, PersonAPI commander, boolean isModule) {
                if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                    effect.applyEffectsToFighterSpawnedByShip(fighter, ship);
                }
            }
        });
    }

    private void applyEffects(final ShipVariantAPI variant, final HullmodAction action) {
        if (variant == null || noSync) return;

        // getCaptain calls getFlagship which updates stats which calls applyEffectsBeforeShipCreation again,
        // there is no getCaptainNoUpdate so this is the next best solution
        noSync = true;
        final VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(variant);
        final ShipHullSpecAPI rootSpec = info == null ? variant.getHullSpec() : info.root.getHullSpec();
        final boolean isModule = info != null && !Objects.equals(info.uid, info.rootUid);
        final PersonAPI commander = info == null ? null : info.commander;
        MasteryUtils.applyAllActiveMasteryEffects(
                commander, rootSpec, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        action.perform(effect, commander, isModule);
                    }
                });
        noSync = false;
    }

    private interface HullmodAction {
        void perform(MasteryEffect effect, PersonAPI commander, boolean isModule);
    }
}
