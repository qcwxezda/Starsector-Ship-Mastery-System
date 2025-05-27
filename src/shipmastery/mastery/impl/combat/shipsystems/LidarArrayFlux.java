package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class LidarArrayFlux extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.LidarArrayFlux).params(getSystemName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(LidarArrayFluxScript.class)) {
            ship.addListener(new LidarArrayFluxScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "lidararray";
    }

    static class LidarArrayFluxScript implements AdvanceableListener {
        final ShipAPI ship;
        final String id;
        final float reduction;

        LidarArrayFluxScript(ShipAPI ship, float reduction, String id) {
            this.ship = ship;
            this.id = id;
            this.reduction = reduction;
        }

        @Override
        public void advance(float amount) {
            if (ship.getSystem().isActive()) {
                Utils.maintainStatusForPlayerShip(ship,
                        id,
                        "graphics/icons/hullsys/lidar_barrage.png",
                        Strings.Descriptions.LidarArrayFluxTitle,
                        String.format(Strings.Descriptions.LidarArrayFluxDesc1, Utils.asPercent(reduction)),
                        false);
                ship.getMutableStats().getBallisticWeaponFluxCostMod().modifyMult(id, 1f - reduction);
            }
            else {
                ship.getMutableStats().getBallisticWeaponFluxCostMod().unmodify(id);
            }
        }
    }
}
