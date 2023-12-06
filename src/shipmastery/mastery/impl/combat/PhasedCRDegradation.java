package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PhasedCRDegradation extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.PhasedCRDegradation).params(
                Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(PhasedCRDegradationScript.class)) {
            ship.addListener(new PhasedCRDegradationScript(ship, 1f - getStrength(ship), id));
        }
    }

    static class PhasedCRDegradationScript implements AdvanceableListener {

        final ShipAPI ship;
        final float mult;
        final String id;

        PhasedCRDegradationScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            if (ship.isPhased()) {
                ship.getMutableStats().getCRLossPerSecondPercent().modifyMult(id, mult);
                ship.setTimeDeployed(ship.getTimeDeployedForCRReduction() - amount * (1f - mult));
            }
            else {
                ship.getMutableStats().getCRLossPerSecondPercent().unmodify(id);
            }
        }
    }
}
