package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import particleengine.Particles;
import shipmastery.fx.EntityBurstEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class HullThresholdDR extends BaseMasteryEffect {

    public static final float DAMAGE_MULT = 0.1f;

    public int getMaxActivations(ShipVariantAPI variant) {
        return switch (variant.getHullSize()) {
            case FRIGATE, DESTROYER -> 1;
            case CRUISER -> 2;
            case CAPITAL_SHIP -> 3;
            default -> 0;
        };
    }

    public float getActivationThreshold(ShipVariantAPI variant) {
        return 1f / (1f + getMaxActivations(variant));
    }

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.HullThresholdDR)
                .params(
                        Utils.asPercent(1f - DAMAGE_MULT),
                        Utils.asFloatOneDecimal(getStrength(selectedVariant)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.HullThresholdDRPost,
                0f,
                Misc.getTextColor(),
                Utils.asPercent(getActivationThreshold(selectedVariant)),
                "" + getMaxActivations(selectedVariant));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(HullThresholdDRScript.class)) {
            ship.addListener(new HullThresholdDRScript(ship, getMaxActivations(ship.getVariant()), getActivationThreshold(ship.getVariant()), getStrength(ship), id));
        }
    }

    static class HullThresholdDRScript implements DamageListener, AdvanceableListener {
        final ShipAPI ship;
        final int maxActivations;
        final float thresholdPerActivation;
        final float duration;
        final String id;
        int timesActivated = 0;
        boolean isActive = false;
        float activeDur = 0f;
        final EntityBurstEmitter emitter;
        final IntervalUtil burstInterval = new IntervalUtil(0.1f, 0.1f);
        final float particleLife = 1f;

        HullThresholdDRScript(ShipAPI ship, int maxActivations, float thresholdPerActivation, float duration, String id) {
            this.ship = ship;
            this.maxActivations = maxActivations;
            this.thresholdPerActivation = thresholdPerActivation;
            this.duration = duration;
            this.id = id;

            emitter = new EntityBurstEmitter(ship, ship.getSpriteAPI(), new Color(255, 155, 88, 255), 12, 25f, particleLife);
            emitter.widthGrowth = -20f;
            emitter.alphaMult = 0.2f;
            emitter.jitterRadius = 10f;
            emitter.enableDynamicAnchoring();

            // In case ship starts with less than full health
            while (timesActivated < maxActivations && ship.getHullLevel() <= 1f - thresholdPerActivation * (timesActivated + 1)) {
                timesActivated++;
            }
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (target != ship || ship.getHullLevel() <= 0f) return;
            float damageFracNeeded = thresholdPerActivation * (timesActivated + 1);
            if (timesActivated < maxActivations && ship.getHullLevel() <= 1f - damageFracNeeded) {
                timesActivated++;
                isActive = true;
                activeDur = 0f;
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);
                ship.getMutableStats().getEngineDamageTakenMult().modifyMult(id, DAMAGE_MULT);
                ship.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, DAMAGE_MULT);
            }
        }

        @Override
        public void advance(float amount) {
            if (isActive) {
                float effectLevel = 1f;

                burstInterval.advance(amount);
                if (burstInterval.intervalElapsed()) {
                    Particles.burst(emitter, 12);
                }

                activeDur += amount;
                if (activeDur >= duration + particleLife) {
                    isActive = false;
                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getEngineDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getWeaponDamageTakenMult().unmodify(id);
                    return;
                }
                else if (activeDur >= duration) {
                    effectLevel = 1f - (activeDur - duration) / particleLife;
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                    ship.getMutableStats().getEngineDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                    ship.getMutableStats().getWeaponDamageTakenMult().modifyMult(id, 1f - (1f - DAMAGE_MULT) * effectLevel);
                }

                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/hullsys/damper_field.png",
                        Strings.Descriptions.HullThresholdDRTitle,
                        String.format(Strings.Descriptions.HullThresholdDRDesc1, (int) (100f * (1f - DAMAGE_MULT) * effectLevel) + "%"),
                        false);
                Global.getSoundPlayer().playLoop("system_damper_loop", ship, 1f, effectLevel, ship.getLocation(), ship.getVelocity());
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 1f;
    }
}
