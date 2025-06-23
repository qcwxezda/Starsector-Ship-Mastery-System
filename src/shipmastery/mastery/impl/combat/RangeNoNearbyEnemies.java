package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class RangeNoNearbyEnemies extends BaseMasteryEffect {

    public static final float[] MAX_RANGE = new float[] {600f, 800f, 1000f, 1250f};

    float getIncreaseRate(ShipVariantAPI variant) {
        return getStrength(variant) / 5f;
    }

    float getDecayRate(ShipVariantAPI variant) {
        return getStrength(variant) / 10f;
    }

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RangeNoNearbyEnemies).params(
                Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.RangeNoNearbyEnemiesPost,
                0f,
                new Color[] {Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asInt(MAX_RANGE[Utils.hullSizeToInt(selectedVariant.getHullSize())]),
                Utils.asPercent(getIncreaseRate(selectedVariant)),
                Utils.asPercent(getDecayRate(selectedVariant)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(RangeNoNearbyEnemiesScript.class)) {
            ship.addListener(new RangeNoNearbyEnemiesScript(ship, getIncreaseRate(ship.getVariant()), getDecayRate(ship.getVariant()), getStrength(ship), id));
        }
    }

    static class RangeNoNearbyEnemiesScript implements AdvanceableListener {

        final ShipAPI ship;
        final float increaseRate;
        final float decayRate;
        final float maxIncrease;
        final String id;
        final IntervalUtil checkerInterval = new IntervalUtil(1f, 1f);
        float currentIncrease = 0f;
        boolean enemyShipNearby = false;

        RangeNoNearbyEnemiesScript(ShipAPI ship, float increaseRate, float decayRate, float maxIncrease, String id) {
            this.ship = ship;
            this.increaseRate = increaseRate;
            this.decayRate = decayRate;
            this.maxIncrease = maxIncrease;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            checkerInterval.advance(amount);
            if (checkerInterval.intervalElapsed()) {
                enemyShipNearby = false;
                // probably faster than using quadtree for big ranges
                for (ShipAPI otherShip : Global.getCombatEngine().getShips()) {
                    if (otherShip.getHitpoints() <= 0f || otherShip.isFighter() || otherShip.getOwner() == ship.getOwner()) continue;
                    if (MathUtils.dist(ship.getLocation(), otherShip.getLocation()) <= MAX_RANGE[Utils.hullSizeToInt(ship.getHullSize())] + ship.getCollisionRadius() + otherShip.getCollisionRadius()) {
                        enemyShipNearby = true;
                        break;
                    }
                }
            }

            if (enemyShipNearby) {
                currentIncrease -= amount * decayRate;
            }
            else {
                currentIncrease += amount * increaseRate;
            }
            currentIncrease = Math.max(0f, Math.min(maxIncrease, currentIncrease));

            if (currentIncrease > 0f) {
                ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(id, 100f * currentIncrease);
                ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(id, 100f * currentIncrease);
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/sensor_array.png",
                        Strings.Descriptions.RangeNoNearbyEnemiesTitle,
                        String.format(Strings.Descriptions.RangeNoNearbyEnemiesDesc1, Utils.asPercentNoDecimal(currentIncrease)),
                        false);
            }
            else {
                ship.getMutableStats().getBallisticWeaponRangeBonus().unmodify(id);
                ship.getMutableStats().getEnergyWeaponRangeBonus().unmodify(id);
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 1f;
    }
}
