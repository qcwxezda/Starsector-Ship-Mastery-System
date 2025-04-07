package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.AcausalDisruptorStats;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class QuantumDisruptorDuration extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float str = getStrength(selectedModule);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.QuantumDisruptorDuration)
                .params(getSystemName(), Utils.asPercent(str), Utils.asPercent(str * 0.1f));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(QuantumDisruptorDurationScript.class)) {
            ship.addListener(new QuantumDisruptorDurationScript(ship, getStrength(ship), getStrength(ship) * 0.1f, id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "acausaldisruptor";
    }

    static class QuantumDisruptorDurationScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float increase;

        QuantumDisruptorDurationScript(ShipAPI ship, float mult, float reduction, String id) {
            this.ship = ship;
            increase = mult;
            ship.getMutableStats().getSystemCooldownBonus().modifyMult(id, 1f - reduction);
        }

        @Override
        public void onFullyActivate() {
            Object target = Global.getCombatEngine().getCustomData().get(ship.getId() + "_acausal_target");
            if (target instanceof ShipAPI targetShip) {
                targetShip.getFluxTracker().stopOverload();
                targetShip.getFluxTracker().beginOverloadWithTotalBaseDuration(AcausalDisruptorStats.DISRUPTION_DUR * (1f + increase));
                ship.useSystem();
            }
        }
    }
}
