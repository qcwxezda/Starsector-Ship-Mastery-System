package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MinimumCR extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MinimumCR)
                                 .params(Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(MinimumCRScript.class)) {
            ship.addListener(new MinimumCRScript(ship, getStrength(ship)));
        }
    }

    static class MinimumCRScript implements AdvanceableListener {
        final ShipAPI ship;
        final float minimum;
        MinimumCRScript(ShipAPI ship, float minimum) {
            this.ship = ship;
            this.minimum = minimum;
        }

        @Override
        public void advance(float amount) {
            ship.setCurrentCR(Math.min(ship.getCRAtDeployment(), Math.max(ship.getCurrentCR(), minimum)));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 3.5f - Utils.hullSizeToInt(spec.getHullSize());
    }
}
