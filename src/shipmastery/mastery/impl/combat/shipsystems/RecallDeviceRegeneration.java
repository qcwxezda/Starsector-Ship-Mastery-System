package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;

public class RecallDeviceRegeneration extends BaseMasteryEffect {

    static final float REPLACEMENT_RATE_INCREASE = 0.1f;
    static final float REGENERATION_CHANCE = 0.3f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RecallDeviceRegeneration)
                                 .params(
                                         Strings.Descriptions.RecallDeviceRegenerationName,
                                         Utils.asPercent(REPLACEMENT_RATE_INCREASE),
                                         Utils.asPercent(REGENERATION_CHANCE));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"recalldevice".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(RecallDeviceRegenerationScript.class)) {
            ship.addListener(new RecallDeviceRegenerationScript());
        }
    }

    public static class RecallDeviceRegenerationScript extends BaseShipSystemListener {
        @Override
        public void onFullyActivate(ShipAPI ship) {
            List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
            if (bays == null || bays.isEmpty()) return;
            for (FighterLaunchBayAPI bay : bays) {
                bay.setCurrRate(Math.min(1f, bay.getCurrRate() + REPLACEMENT_RATE_INCREASE));
                int regen = 0;
                // Fighter wings are capped at 6, so this is always faster than trying to
                // write a binomial pdf function
                for (int i = 0; i < bay.getNumLost(); i++) {
                    if (Math.random() <= REGENERATION_CHANCE) {
                        regen++;
                    }
                }
                bay.setFastReplacements(bay.getFastReplacements() + regen);
            }
        }
    }
}
