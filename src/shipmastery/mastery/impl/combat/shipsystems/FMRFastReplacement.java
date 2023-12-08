package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Collections;
import java.util.List;

public class FMRFastReplacement extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.FMRFastReplacement)
                .params(systemName, Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"fastmissileracks".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(FMRFastReplacementScript.class)) {
            ship.addListener(new FMRFastReplacementScript(ship, getStrength(ship)));
        }
    }

    static class FMRFastReplacementScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float regenChance;

        FMRFastReplacementScript(ShipAPI ship, float regenChance) {
            this.ship = ship;
            this.regenChance = regenChance;
        }

        @Override
        public void onActivate() {
            if (Misc.random.nextFloat() <= regenChance) {
                List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
                Collections.shuffle(bays);
                for (FighterLaunchBayAPI bay : bays) {
                    if (bay.getNumLost() > bay.getFastReplacements()) {
                        bay.setFastReplacements(bay.getFastReplacements() + 1);
                        return;
                    }
                }
            }
        }
    }
}
