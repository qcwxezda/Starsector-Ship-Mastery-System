package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class LidarArrayRange extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.LidarArrayRange).params(getSystemName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(LidarArrayRangeScript.class)) {
            ship.addListener(new LidarArrayRangeScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "lidararray";
    }

    record LidarArrayRangeScript(ShipAPI ship, float bonus, String id) implements AdvanceableListener {
        @Override
            public void advance(float amount) {
                if (!ship.getSystem().isActive()) {
                    Utils.maintainStatusForPlayerShip(ship,
                            id,
                            "graphics/icons/hullsys/lidar_barrage.png",
                            Strings.Descriptions.LidarArrayRangeTitle,
                            String.format(Strings.Descriptions.LidarArrayRangeDesc1, Utils.asPercent(bonus)),
                            false);
                    ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, 100f * bonus);
                    ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, 100f * bonus);
                } else {
                    ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
                    ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
                }
            }
        }
}
