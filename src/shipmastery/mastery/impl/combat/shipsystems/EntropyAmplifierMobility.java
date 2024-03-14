package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.EntropyAmplifierStats;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EntropyAmplifierMobility extends ShipSystemEffect {
    public static final String ENTROPY_AMPLIFIER_ID = "entropyamplifier";

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EntropyAmplifierMobility)
                                 .params(getSystemName(),
                                         Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(EntropyAmplifierMobilityScript.class)) {
            ship.addListener(new EntropyAmplifierMobilityScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return ENTROPY_AMPLIFIER_ID;
    }

    static class EntropyAmplifierMobilityScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final float strength;
        final String id;
        EntropyAmplifierStats.TargetData curTargetData;

        EntropyAmplifierMobilityScript(ShipAPI ship, float strength, String id) {
            this.ship = ship;
            this.strength = strength;
            this.id = id;
        }

        @Override
        public void onFullyActivate() {
            Object targetDataObj = Global.getCombatEngine().getCustomData().get(ship.getId() + "_entropy_target_data");
            if (targetDataObj == null) return;

            curTargetData = ((EntropyAmplifierStats.TargetData) targetDataObj);
        }
        @Override
        public void advance(float amount) {
            if (curTargetData == null) return;

            ShipAPI target = curTargetData.target;
            if (curTargetData.currDamMult <= 1f) {
                target.getMutableStats().getMaxSpeed().unmodify(ENTROPY_AMPLIFIER_ID);
                target.getMutableStats().getTurnAcceleration().unmodify(ENTROPY_AMPLIFIER_ID);
                target.getMutableStats().getMaxTurnRate().unmodify(ENTROPY_AMPLIFIER_ID);
                target.getMutableStats().getAcceleration().unmodify(ENTROPY_AMPLIFIER_ID);
                target.getMutableStats().getDeceleration().unmodify(ENTROPY_AMPLIFIER_ID);
                curTargetData = null;
                return;
            }

            float effectLevel = ship.getSystem().getEffectLevel();
            float mult = 1f - effectLevel*strength;

            Utils.maintainStatusForPlayerShip(target,
                                              ENTROPY_AMPLIFIER_ID,
                                              ship.getSystem().getSpecAPI().getIconSpriteName(),
                                              ship.getSystem().getDisplayName(),
                                              String.format(Strings.Descriptions.EntropyAmplifierMobilityDesc1, Utils.asPercentNoDecimal(effectLevel*strength)),
                                              true);

            // Use ENTROPY_AMPLIFIER_ID so that the effect doesn't stack
            target.getMutableStats().getMaxSpeed().modifyMult(ENTROPY_AMPLIFIER_ID, mult);
            target.getMutableStats().getTurnAcceleration().modifyMult(ENTROPY_AMPLIFIER_ID, mult);
            target.getMutableStats().getMaxTurnRate().modifyMult(ENTROPY_AMPLIFIER_ID, mult);
            target.getMutableStats().getAcceleration().modifyMult(ENTROPY_AMPLIFIER_ID, mult);
            target.getMutableStats().getDeceleration().modifyMult(ENTROPY_AMPLIFIER_ID, mult);
        }
    }
}
