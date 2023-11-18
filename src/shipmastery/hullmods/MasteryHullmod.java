package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

public class MasteryHullmod extends BaseHullMod implements HullModFleetEffect {

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
            fm.getVariant().setSource(VariantSource.REFIT);
        }
        // Bypass the arbitrary checks in removeMod since we're adding it back anyway
        fm.getVariant().getHullMods().remove("sms_masteryHandler");
        fm.getVariant().getHullMods().add("sms_masteryHandler");
    }

    @Override
    public void applyEffectsBeforeShipCreation(final ShipAPI.HullSize hullSize, final MutableShipStatsAPI stats, final String id) {
        if (stats == null || stats.getVariant() == null) return;
        ShipHullSpecAPI spec = Utils.getBaseHullSpec(stats.getVariant().getHullSpec());
        MasteryUtils.applyAllActiveMasteryEffects(spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                effect.applyEffectsBeforeShipCreation(hullSize, stats, id);
            }
        });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        if (ship == null) return;
        ShipHullSpecAPI spec = Utils.getBaseHullSpec(ship.getHullSpec());
        MasteryUtils.applyAllActiveMasteryEffects(spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                effect.applyEffectsAfterShipCreation(ship, id);
            }
        });
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        if (ship == null) return;
        ShipHullSpecAPI spec = Utils.getBaseHullSpec(ship.getHullSpec());
        MasteryUtils.applyAllActiveMasteryEffects(spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                effect.applyEffectsToFighterSpawnedByShip(fighter, ship, id);
            }
        });
    }
}
