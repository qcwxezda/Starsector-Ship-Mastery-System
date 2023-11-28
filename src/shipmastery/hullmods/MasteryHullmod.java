package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.fleet.FleetData;
import shipmastery.ShipMasteryNPC;
import shipmastery.campaign.RefitHandler;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.CommanderLookup;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.util.HashSet;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;

public class MasteryHullmod extends BaseHullMod implements HullModFleetEffect {

    /**
     * Two tags added to variants to indicate that they are a module (O(1) lookup)
     * and to record what hull spec the variant's base ship has (O(|tags|) search).
     * This is not feasible with a lookup table (variant id -> base hull spec) because different hulls may
     * have the same variant id as a module.
     */
    private static final String MODULE_INDICATOR_TAG = "shipmastery_is_module";
    private static final String MODULE_SOURCE_TAG = "shipmastery_module_root_";

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
    public void onFleetSync(final CampaignFleetAPI fleet) {
        if (fleet == null) {
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
                CommanderLookup.addCommander(commander.getId(), commander);
                for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
                    if (fm.getVariant().isStockVariant() || fm.getVariant().isEmptyHullVariant()) {
                        fm.setVariant(fm.getVariant().clone(), false, false);
                    }
                    // Put the fleet commander id in a tag as there's no other way to retrieve it from a variant
                    CommanderLookup.tagVariant(fm.getVariant(), commander);
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
                    addHandlerMod(fm.getVariant(), fm.getHullId());
                }

                if (fleet.isPlayerFleet()) {
                    for (FleetMemberAPI fm : Utils.getMembersNoSync(fleet)) {
                        Utils.fixVariantInconsistencies(fm.getVariant());
                    }
                }
            }
        }, 0f);
    }

    void addHandlerMod(ShipVariantAPI variant, String rootSpecId) {
        if (variant.isStockVariant()) {
            variant.setSource(VariantSource.REFIT);
        }
        // Bypass the arbitrary checks in removeMod since we're adding it back anyway
        variant.getHullMods().remove("sms_masteryHandler");
        variant.getHullMods().add("sms_masteryHandler");
        variant.getPermaMods().add("sms_masteryHandler");
        // This does nothing except set hasOpAffectingMods to null, forcing variants to
        // recompute their statsForOpCosts
        // (Normally this is naturally set when a hullmod is added or removed)
        variant.addPermaMod("sms_masteryHandler");
        // Add the tracker to any modules as well
        for (String id : variant.getModuleSlots()) {
            ShipVariantAPI moduleVariant = variant.getModuleVariant(id);
            moduleVariant.addTag(MODULE_INDICATOR_TAG);
            moduleVariant.addTag(MODULE_SOURCE_TAG + rootSpecId);
            moduleVariant.setHullVariantId(moduleVariant.getHullVariantId());
            addHandlerMod(variant.getModuleVariant(id), rootSpecId);
        }
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    /**
     * returns (hull spec of the root ship, whether this variant is a module)
     */
    private Pair<ShipHullSpecAPI, Boolean> getRootHullSpec(ShipVariantAPI variant) {
        ShipHullSpecAPI rootHullSpec = null;
        boolean isModule = false;
        if (variant.hasTag(MODULE_INDICATOR_TAG)) {
            for (String tag : variant.getTags()) {
                if (tag.startsWith(MODULE_SOURCE_TAG)) {
                    String specId = tag.substring(MODULE_SOURCE_TAG.length());
                    rootHullSpec = Global.getSettings().getHullSpec(specId);
                    isModule = true;
                    break;
                }
            }
        } else {
            rootHullSpec = Utils.getRestoredHullSpec(variant.getHullSpec());
        }
        return new Pair<>(rootHullSpec, isModule);
    }

    @Override
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats,
                                               final String id) {
        if (stats == null || stats.getVariant() == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(stats.getVariant());
        final PersonAPI commander = CommanderLookup.getCommander(stats.getVariant());

        MasteryUtils.applyAllActiveMasteryEffects(
                commander,
                rootHullData.one, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                            effect.applyEffectsBeforeShipCreation(hullSize, stats);
                            // For display purposes only
                            if (commander != null && stats.getFleetMember() != null && Objects.equals(commander, stats.getFleetMember().getCaptain())) {
                                effect.onFlagshipStatusGained(commander, stats, null);
                            }
                        }
                    }
                });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        if (ship == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(ship.getVariant());
        // ship.getFleetMember() is null,
        // ship.getFleetCommander() doesn't work for merged fleets with multiple commanders
        // ship.getMutableStats().getFleetMember().getFleetCommanderForStats() actually differentiates between commanders in merged fleets
        // However, it's null for player ships...
        final PersonAPI commander = CommanderLookup.getCommander(ship.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(
                commander,
                rootHullData.one, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                            effect.applyEffectsAfterShipCreation(ship);
                            // For display purposes only
                            if (commander != null && Objects.equals(commander, ship.getCaptain())) {
                                effect.onFlagshipStatusGained(commander, ship.getMutableStats(), null);
                            }
                        }
                    }
                });

    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        if (ship == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(ship.getVariant());
        final PersonAPI commander = CommanderLookup.getCommander(ship.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(
                commander,
                rootHullData.one, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                            effect.applyEffectsToFighterSpawnedByShip(fighter, ship);
                        }
                    }
                });
    }
}
