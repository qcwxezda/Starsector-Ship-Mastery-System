package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Pair;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

public class MasteryHullmod extends BaseHullMod implements HullModFleetEffect {

    /** Two tags added to variants to indicate that they are a module (O(1) lookup)
     *  and to record what hull spec the variant's base ship has (O(|tags|) search).
     *  This is not feasible with a lookup table (variant id -> base hull spec) because different hulls may
     *  have the same variant id as a module. */
    private static final String MODULE_INDICATOR_TAG = "shipmastery_is_module";
    private static final String MODULE_SOURCE_TAG = "shipmastery_module_root_";

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {return true;}

    @Override
    public void onFleetSync(final CampaignFleetAPI fleet) {
        if (fleet == null || !fleet.isPlayerFleet()) {
            return;
        }
        // Make sure every ship in the player's fleet has this hullmod installed
        // Do it outside this call stack as changing the ship immediately actually causes some unexpected changes
        // i.e. the "you just purchased a ship" screen no longer appears as the purchased ship is
        // no longer the same as the one in the player's fleet
        DeferredActionPlugin.performLater(new Action() {
            @Override
            public void perform() {
                for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
                    addHandlerMod(fm);
                }
            }
        }, 0f);
    }

    void addHandlerMod(FleetMemberAPI fm) {
        if (fm.getVariant().isStockVariant()) {
            fm.setVariant(fm.getVariant().clone(), false, false);
        }
        addHandlerMod(fm.getVariant(), fm.getHullId());
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
            moduleVariant.setHullVariantId(moduleVariant.getHullVariantId());
            addHandlerMod(variant.getModuleVariant(id), rootSpecId);
        }
    }

    /** returns (hull spec of the root ship, whether this variant is a module) */
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
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats, final String id) {
        if (stats == null || stats.getVariant() == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(stats.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(rootHullData.one, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                    effect.applyEffectsBeforeShipCreation(hullSize, stats, id);
                }
            }
        });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        if (ship == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(ship.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(rootHullData.one, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                    effect.applyEffectsAfterShipCreation(ship, id);
                }
            }
        });
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        if (ship == null) return;
        final Pair<ShipHullSpecAPI, Boolean> rootHullData = getRootHullSpec(ship.getVariant());
        MasteryUtils.applyAllActiveMasteryEffects(rootHullData.one, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                if (!rootHullData.two || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                    effect.applyEffectsToFighterSpawnedByShip(fighter, ship, id);
                }
            }
        });
    }
}
