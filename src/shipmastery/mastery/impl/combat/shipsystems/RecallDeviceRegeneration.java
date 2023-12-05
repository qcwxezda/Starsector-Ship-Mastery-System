package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;

public class RecallDeviceRegeneration extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RecallDeviceRegeneration)
                                 .params(
                                         systemName,
                                         Utils.asPercent(getStrengthForPlayer() / 6f),
                                         Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"recalldevice".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(RecallDeviceRegenerationScript.class)) {
            ship.addListener(new RecallDeviceRegenerationScript(ship, getStrength(ship) / 6f, getStrength(ship)));
        }
    }

    public static class RecallDeviceRegenerationScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float replacementRateBoost;
        final float regenerationChance;

        RecallDeviceRegenerationScript(ShipAPI ship, float replacementRateBoost, float regenerationChance) {
            this.ship = ship;
            this.regenerationChance = regenerationChance;
            this.replacementRateBoost = replacementRateBoost;
        }

        @Override
        public void onFullyActivate() {
            List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
            if (bays == null || bays.isEmpty()) return;
            for (FighterLaunchBayAPI bay : bays) {
                bay.setCurrRate(Math.min(1f, bay.getCurrRate() + replacementRateBoost));
                int regen = 0;
                // Fighter wings are capped at 6, so this is always faster than trying to
                // write a binomial pdf function
                for (int i = 0; i < bay.getNumLost(); i++) {
                    if (Math.random() <= regenerationChance) {
                        regen++;
                    }
                }
                bay.setFastReplacements(bay.getFastReplacements() + regen);
            }
        }
    }
}
