package shipmastery.mastery.impl.unused;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class RangeNotMoving extends BaseMasteryEffect {

    public static final float INCREASE_SPEED_LIMIT = 5f, DECAY_SPEED_LIMIT = 10f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.RangeNotMoving)
                .params(Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedVariant);
        tooltip.addPara(
                Strings.Descriptions.RangeNotMovingPost,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(strength / 5f),
                Utils.asInt(INCREASE_SPEED_LIMIT),
                Utils.asPercent(strength / 15f),
                Utils.asInt(DECAY_SPEED_LIMIT));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(RangeNotMovingScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new RangeNotMovingScript(ship, strength, strength / 5f, strength / 15f, id));
        }
    }

    static class RangeNotMovingScript implements AdvanceableListener {

        final ShipAPI ship;
        final float maxIncrease;
        final float increasePerSecond, decreasePerSecond;
        final String id;
        float currentIncrease = 0f;

        RangeNotMovingScript(ShipAPI ship, float maxIncrease, float increasePerSecond, float decreasePerSecond, String id) {
            this.ship = ship;
            this.maxIncrease = maxIncrease;
            this.increasePerSecond = increasePerSecond;
            this.decreasePerSecond = decreasePerSecond;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            if (Global.getCurrentState() != GameState.COMBAT) return;
            float moveSpeed = ship.getVelocity().length();
            if (moveSpeed <= INCREASE_SPEED_LIMIT) {
                currentIncrease += increasePerSecond * amount;
                currentIncrease = Math.min(maxIncrease, currentIncrease);
            }
            if (moveSpeed > DECAY_SPEED_LIMIT) {
                currentIncrease -= decreasePerSecond * amount;
                currentIncrease = Math.max(0f, currentIncrease);
            }

            if (currentIncrease > 0f) {
                ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, currentIncrease * 100f);
                ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, currentIncrease * 100f);

                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/sensor_array.png",
                        Strings.Descriptions.RangeNotMovingTitle,
                        String.format(Strings.Descriptions.RangeNotMovingDesc1, Utils.asPercentNoDecimal(currentIncrease)),
                        false);
            }
            else {
                ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
                ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 0f;
    }
}
