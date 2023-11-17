package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
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
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (fleet == null || !fleet.isPlayerFleet()) {
            return;
        }
        // Make sure every ship in the player's fleet has this hullmod installed
        // The mastery hullmod should be the last hullmod in every ship's hullmod list
        // in order to accommodate effects that check for other hullmods
        // Therefore, we remove and reinsert the hullmod each time the fleet syncs
        // (hullmods are applied in insertion order, backing data structure is LinkedHashSet)
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            fm.getVariant().removePermaMod("sms_masteryHandler");
            addHandlerMod(fm);
        }
    }

    void addHandlerMod(FleetMemberAPI fm) {
        if (fm.getVariant().isStockVariant()) {
            fm.setVariant(fm.getVariant().clone(), false, false);
            fm.getVariant().setSource(VariantSource.REFIT);
        }
        fm.getVariant().addPermaMod("sms_masteryHandler", false);
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
