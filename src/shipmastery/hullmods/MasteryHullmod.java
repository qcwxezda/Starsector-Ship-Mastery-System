package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.VariantSource;
import shipmastery.Settings;
import shipmastery.mastery.MasteryEffect;

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
        applyAllMasteryEffects(stats.getVariant().getHullSpec(), new MasteryEffectAction() {
            @Override
            public void perform(MasteryEffect effect, int level) {
                effect.applyEffectsBeforeShipCreation(hullSize, stats, makeId(effect, level));
            }
        });
    }

    @Override
    public void applyEffectsAfterShipCreation(final ShipAPI ship, final String id) {
        if (ship == null) return;
        applyAllMasteryEffects(ship.getHullSpec(), new MasteryEffectAction() {
            @Override
            public void perform(MasteryEffect effect, int level) {
                effect.applyEffectsAfterShipCreation(ship, makeId(effect, level));
            }
        });
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(final ShipAPI fighter, final ShipAPI ship, final String id) {
        if (ship == null) return;
        applyAllMasteryEffects(ship.getHullSpec(), new MasteryEffectAction() {
            @Override
            public void perform(MasteryEffect effect, int level) {
                effect.applyEffectsToFighterSpawnedByShip(fighter, ship, makeId(effect, level));
            }
        });
    }

    @Override
    public void advanceInCampaign(final FleetMemberAPI member, final float amount) {
        if (member == null) return;
        applyAllMasteryEffects(member.getHullSpec(), new MasteryEffectAction() {
            @Override
            public void perform(MasteryEffect effect, int level) {
                effect.advanceInCampaign(member, amount);
            }
        });
    }

    @Override
    public void advanceInCombat(final ShipAPI ship, final float amount) {
        if (ship == null) return;
        applyAllMasteryEffects(ship.getHullSpec(), new MasteryEffectAction() {
            @Override
            public void perform(MasteryEffect effect, int level) {
                effect.advanceInCombat(ship, amount);
            }
        });
    }

    void applyAllMasteryEffects(ShipHullSpecAPI spec, MasteryEffectAction action) {
        // Not initialized yet, return
        if (Settings.MASTERY_TABLE == null) {
            return;
        }

        int masteryLevel = Settings.getMasteryLevel(spec);
        for (int i = 1; i <= masteryLevel; i++) {
            MasteryEffect effect = Settings.getMasteryEffect(spec, i);
            action.perform(effect, i);
        }
    }

    interface MasteryEffectAction {
        void perform(MasteryEffect effect, int level);
    }

    String makeId(MasteryEffect effect, int level) {
        return Settings.getId(effect.getClass()) + level;
    }
}
