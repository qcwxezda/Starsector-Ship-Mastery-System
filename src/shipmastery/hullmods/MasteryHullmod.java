package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import shipmastery.ShipMasteryNPC;
import shipmastery.campaign.FleetHandler;
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


    public static boolean fixPlayerVariantsNextSync = false;

    @Override
    public void onFleetSync(final CampaignFleetAPI fleet) {
        if (noSync || fleet == null) {
            return;
        }
        if (!fleet.isPlayerFleet()) return;

        // Make sure every ship in the fleet has this hullmod installed
        // Do it outside this call stack as changing the ship immediately actually causes some unexpected changes
        // i.e. the "you just purchased a ship" screen no longer appears as the purchased ship is
        // no longer the same as the one in the player's fleet
        DeferredActionPlugin.performLater(new Action() {
            @Override
            public void perform() {
                for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
                    if (fixPlayerVariantsNextSync) {
                        // This just sets hasOpAffectingMods to null, forcing the variant to
                        // recompute its statsForOpCosts (e.g. number of hangar bays)
                        // (Normally this is naturally set when a hullmod is manually added or removed)
                        fm.getVariant().addPermaMod( "sms_masteryHandler");
                        Utils.fixVariantInconsistencies(fm.getVariant());
                    }
                    ShipVariantAPI variant = fm.getVariant();
                    if (!variant.hasHullMod("sms_masteryHandler") || VariantLookup.getVariantInfo(variant) == null) {
                        fm.setVariant(FleetHandler.addHandlerMod(variant, variant, Global.getSector().getPlayerPerson()), false, false);
                    }
                }
                fixPlayerVariantsNextSync = false;
            }
        }, 0f);



        // Ignore NPC fleets that aren't interacting with the player
//        if (!fleet.isPlayerFleet()) {
//            if (fleet.getBattle() == null || !fleet.getBattle().isPlayerInvolved()) {
//                return;
//            }
//        }

//        DeferredActionPlugin.performLater(new Action() {
//            @Override
//            public void perform() {
//                // Guaranteed not null
//                PersonAPI commander = fleet.getCommander();
//                for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
//                    ShipVariantAPI variant = fm.getVariant();
//
//                }
//
//                if (fleet.isPlayerFleet() && fixPlayerVariantsNextSync) {
//                    for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
//                        Utils.fixVariantInconsistencies(fm.getVariant());
//                    }
//                    fixPlayerVariantsNextSync = false;
//                }
//            }
//        }, 0f);
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
