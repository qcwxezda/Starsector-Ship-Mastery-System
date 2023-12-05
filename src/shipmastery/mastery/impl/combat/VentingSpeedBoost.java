package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class VentingSpeedBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.VentingSpeedBoost).params(Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(VentingSpeedBoostScript.class)) {
            ship.addListener(new VentingSpeedBoostScript(ship, getStrength(ship), id));
        }
    }

    static class VentingSpeedBoostScript implements AdvanceableListener {

        final ShipAPI ship;
        final float increase;
        final String id;

        VentingSpeedBoostScript(ShipAPI ship, float increase, String id) {
            this.ship = ship;
            this.increase = increase;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            if (ship.getFluxTracker().isVenting()) {
                ship.getMutableStats().getMaxSpeed().modifyPercent(id, 100f * increase);
                ship.getMutableStats().getMaxTurnRate().modifyPercent(id, 100f * increase);
                ship.getMutableStats().getAcceleration().modifyPercent(id, 100f * increase);
                ship.getMutableStats().getDeceleration().modifyPercent(id, 100f * increase);
                ship.getMutableStats().getTurnAcceleration().modifyPercent(id, 100f * increase);
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/infernium_injector.png",
                        Strings.Descriptions.VentingSpeedBoostTitle,
                        String.format(Strings.Descriptions.VentingSpeedBoostDesc1, Utils.asPercent(increase)),
                        false);
            }
            else {
                ship.getMutableStats().getMaxSpeed().unmodify(id);
                ship.getMutableStats().getMaxTurnRate().unmodify(id);
                ship.getMutableStats().getAcceleration().unmodify(id);
                ship.getMutableStats().getDeceleration().unmodify(id);
                ship.getMutableStats().getTurnAcceleration().unmodify(id);
            }
        }
    }
}
