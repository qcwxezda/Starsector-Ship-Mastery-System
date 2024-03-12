package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class HEFShieldEfficiency extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.HEFShieldEfficiency).params(systemName, Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getShield() == null || ship.getSystem() == null || !"highenergyfocus".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(HEFShieldEfficiencyScript.class)) {
            ship.addListener(new HEFShieldEfficiencyScript(ship, getStrength(ship), id));
        }
    }

    static class HEFShieldEfficiencyScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float strength;
        final String id;
        final Color originalColor;

        HEFShieldEfficiencyScript(ShipAPI ship, float strength, String id) {
            this.ship = ship;
            this.strength = strength;
            this.id = id;
            originalColor = ship.getShield().getInnerColor();
        }

        @Override
        public void advanceWhileOn(float amount) {
            float mult = strength * ship.getSystem().getEffectLevel();

            if (mult > 0f) {
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f - mult);
                ship.getShield().setInnerColor(Utils.mixColor(originalColor, ship.getShield().getRingColor(), 0.33f * mult / strength));
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/fortress_shield.png",
                        Strings.Descriptions.HEFShieldEfficiencyTitle,
                        String.format(
                                Strings.Descriptions.HEFShieldEfficiencyDesc1, Utils.asPercentNoDecimal(mult)),
                        false);
            }
            else {
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                ship.getShield().setInnerColor(originalColor);
            }

        }
    }
}
