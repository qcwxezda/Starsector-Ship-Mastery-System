package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ShieldEfficiencyHardFlux extends BaseMasteryEffect {

    public static final float MIN_THRESHOLD = 0.2f, MAX_THRESHOLD = 0.9f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ShieldEfficiencyHardFlux).params(Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ShieldEfficiencyHardFluxPost, 0f, Misc.getTextColor(), Utils.asPercent(MIN_THRESHOLD), Utils.asPercent(MAX_THRESHOLD));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getShield() == null) return;
        if (!ship.hasListenerOfClass(ShieldEfficiencyHardFluxScript.class)) {
            ship.addListener(new ShieldEfficiencyHardFluxScript(ship, getStrength(ship), id));
        }
    }

    static class ShieldEfficiencyHardFluxScript implements AdvanceableListener {
        final ShipAPI ship;
        final float maxStrength;
        final Color origShieldColor;
        final String id;

        ShieldEfficiencyHardFluxScript(ShipAPI ship, float maxStrength, String id) {
            this.ship = ship;
            this.maxStrength = maxStrength;
            origShieldColor = ship.getShield().getInnerColor();
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            float fluxLevel = ship.getHardFluxLevel();
            float strength = maxStrength * (fluxLevel - MIN_THRESHOLD) / (MAX_THRESHOLD - MIN_THRESHOLD);
            strength = MathUtils.clamp(strength, 0f, maxStrength);

            if (strength > 0f) {
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f - strength);
                //ship.getShield().setInnerColor(Utils.mixColor(origShieldColor, ship.getShield().getRingColor(), 0.33f * strength / maxStrength));
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/fortress_shield.png",
                        Strings.Descriptions.ShieldEfficiencyHardFluxTitle,
                        String.format(Strings.Descriptions.ShieldEfficiencyHardFluxDesc1, Utils.asPercentNoDecimal(strength)),
                        false);
            }
            else {
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                //ship.getShield().setInnerColor(origShieldColor);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.hasShield(spec)) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getFluxCapacity(), 5000f, false);
    }
}
