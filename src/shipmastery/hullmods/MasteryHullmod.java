package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;

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
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            if (!fm.getVariant().hasHullMod("sms_masteryHandler")) {
                addHandlerMod(fm);
            }
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
        ShipHullSpecAPI spec = stats.getVariant().getHullSpec();
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
        ShipHullSpecAPI spec = ship.getHullSpec();
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
        ShipHullSpecAPI spec = ship.getHullSpec();
        MasteryUtils.applyAllActiveMasteryEffects(spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                effect.applyEffectsToFighterSpawnedByShip(fighter, ship, id);
            }
        });
    }

    @Override
    public void advanceInCampaign(final FleetMemberAPI member, final float amount) {
        if (member == null) return;
        ShipHullSpecAPI spec = member.getHullSpec();
        MasteryUtils.applyAllActiveMasteryEffects(spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                effect.advanceInCampaign(member, amount, id);
            }
        });
    }

    @Override
    public void advanceInCombat(final ShipAPI ship, final float amount) {
        if (ship == null) return;
        ShipHullSpecAPI spec = ship.getHullSpec();
        MasteryUtils.applyAllActiveMasteryEffects(spec, new MasteryUtils.MasteryAction() {
            @Override
            public void perform(MasteryEffect effect, String id) {
                effect.advanceInCombat(ship, amount, id);
            }
        });
    }
}
