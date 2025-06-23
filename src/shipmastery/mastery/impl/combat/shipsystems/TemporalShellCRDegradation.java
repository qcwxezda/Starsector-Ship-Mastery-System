package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class TemporalShellCRDegradation extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.TemporalShellCRDegradation).params(
                getSystemName(), Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(TemporalShellCRDegradationScript.class)) {
            ship.addListener(new TemporalShellCRDegradationScript(ship, 1f - getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "temporalshell";
    }

    record TemporalShellCRDegradationScript(ShipAPI ship, float mult, String id) implements AdvanceableListener {
        @Override
            public void advance(float amount) {
                if (!ship.areSignificantEnemiesInRange()) return;
                if (ship.getSystem().isActive()) {
                    ship.getMutableStats().getCRLossPerSecondPercent().modifyMult(id, mult);
                    ship.setTimeDeployed(ship.getTimeDeployedForCRReduction() - amount * (1f - mult));
                } else {
                    ship.getMutableStats().getCRLossPerSecondPercent().unmodify(id);
                }
            }
        }
}
