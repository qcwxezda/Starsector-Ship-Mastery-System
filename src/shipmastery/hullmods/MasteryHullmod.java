package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Pair;
import shipmastery.ShipMasteryNPC;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;

public class MasteryHullmod extends BaseHullMod implements HullModFleetEffect {

    /**
     * Two tags added to variants to indicate that they are a module (O(1) lookup)
     * and to record what hull spec the variant's base ship has (O(|tags|) search).
     * This is not feasible with a lookup table (variant id -> base hull spec) because different hulls may
     * have the same variant id as a module.
     */
    private static final String MODULE_INDICATOR_TAG = "shipmastery_is_module";
    private static final String MODULE_SOURCE_TAG = "shipmastery_module_root_";
    private static final String NPC_INDICATOR_TAG = "shipmastery_is_npc";

    public static String LAST_SEEN_COMBINED_NPC_FLEET_ID;


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
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return;
        }
        if (!fleet.isPlayerFleet()) {
            BattleAPI battle = fleet.getBattle();
            if (battle == null) return;
            List<CampaignFleetAPI> nonPlayerSide = battle.getNonPlayerSide();
            CampaignFleetAPI combinedTwo = battle.getCombinedTwo();
            if (nonPlayerSide == null ||
                    combinedTwo == null ||
                    Objects.equals(LAST_SEEN_COMBINED_NPC_FLEET_ID, combinedTwo.getId())) return;
            ShipMasteryNPC.generateMasteryLevelsForNPCFleet(nonPlayerSide);
            LAST_SEEN_COMBINED_NPC_FLEET_ID = combinedTwo.getId();
            fleet = combinedTwo;
        }
        // Make sure every ship in the player's fleet has this hullmod installed
        // Do it outside this call stack as changing the ship immediately actually causes some unexpected changes
        // i.e. the "you just purchased a ship" screen no longer appears as the purchased ship is
        // no longer the same as the one in the player's fleet
        final CampaignFleetAPI finalFleet = fleet;
        DeferredActionPlugin.performLater(new Action() {
            @Override
            public void perform() {
                for (FleetMemberAPI fm : finalFleet.getFleetData().getMembersListCopy()) {
                    if (fm.getVariant().isStockVariant()) {
                        fm.setVariant(fm.getVariant().clone(), false, false);
                    }
                    if (!finalFleet.isPlayerFleet()) {
                        fm.getVariant().addTag(NPC_INDICATOR_TAG);
                        // Necessary to remove all indicators because the same fleet member may have a different
                        // commander based on what other fleets joined in, and the masteries should be based
                        // on the commander and not the fleet member
                        for (int i = 1; i <= 9; i++) {
                            fm.getVariant().removeMod("sms_npcIndicator" + i);
                        }
                        NavigableMap<Integer, Boolean>
                                levels = ShipMasteryNPC.CACHED_NPC_FLEET_MASTERIES.get(
                                new Pair<>(fm.getFleetCommander().getId(),
                                           Utils.getRestoredHullSpec(fm.getHullSpec())));
                        if (levels != null && !levels.isEmpty()) {
                            int maxLevel = levels.lastEntry().getKey();
                            if (maxLevel >= 1) {
                                maxLevel = Math.min(maxLevel, 9);
                                fm.getVariant().addMod("sms_npcIndicator" + maxLevel);
                            }
                        }
                    }
                    addHandlerMod(fm.getVariant(), fm.getHullId());
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
        // Add the tracker to any modules as well
        for (String id : variant.getModuleSlots()) {
            ShipVariantAPI moduleVariant = variant.getModuleVariant(id);
            moduleVariant.addTag(MODULE_INDICATOR_TAG);
            moduleVariant.addTag(MODULE_SOURCE_TAG + rootSpecId);
            if (variant.hasTag(NPC_INDICATOR_TAG)) {
                moduleVariant.addTag(NPC_INDICATOR_TAG);
            }
            moduleVariant.setHullVariantId(moduleVariant.getHullVariantId());
            addHandlerMod(variant.getModuleVariant(id), rootSpecId);
        }
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
        MasteryUtils.applyAllActiveMasteryEffects(
                stats.getFleetMember() == null ? null : stats.getFleetMember().getFleetCommander(),
                rootHullData.one, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                            effect.applyEffectsBeforeShipCreation(hullSize, stats);
                        }
                    }
                });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        if (ship == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(ship.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(
                ship.getFleetMember() == null ? null : ship.getFleetMember().getFleetCommander(),
                rootHullData.one, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                            effect.applyEffectsAfterShipCreation(ship);
                            // This is only for refit screen display purposes, so it's hardcoded to only affect the player fleet
                            if (Global.getSector().getPlayerPerson().equals(ship.getCaptain())) {
                                effect.onFlagshipStatusGained(ship);
                            }
                        }
                    }
                });

    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        if (ship == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(ship.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(
                ship.getFleetMember() == null ? null : ship.getFleetMember().getFleetCommander(),
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
