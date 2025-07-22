package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.ai.missile.RocketAI;
import particleengine.Particles;
import shipmastery.combat.ai.LOSMissileAI;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.config.Settings;
import shipmastery.fx.TrailEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class TorpedoTracking extends BaseMasteryEffect {

    public static final float SPEED_MULT = 0.8f;
    public static final float MIN_DAMAGE = 500f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.TorpedoTracking)
                                 .params(Utils.asInt(MIN_DAMAGE), Utils.asPercent(getStrengthForPlayer() * 0.1f), Utils.asPercent(1f - SPEED_MULT))
                                 .colors(Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.TorpedoTrackingPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asFloatOneDecimal(getStrengthForPlayer()));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(TorpedoTrackingScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new TorpedoTrackingScript(ship, strength, strength * 0.1f, id));
        }
    }

    record TorpedoTrackingScript(ShipAPI ship, float turnRate, float hpBoost,
                                 String id) implements ProjectileCreatedListener {
        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!(proj instanceof MissileAPI missile)) return;
            if (!(missile.getUnwrappedMissileAI() instanceof RocketAI)) return;
            if (proj.getBaseDamageAmount() < MIN_DAMAGE) return;
            TrailEmitter trail = new TrailEmitter(missile);
            Color explosionColor = missile.getSpec().getExplosionColor();
            trail.color = new Color(explosionColor.getRed() / 255f, explosionColor.getGreen() / 255f, explosionColor.getBlue() / 255f, 0.3f);
            trail.length = 40f;
            trail.width = 20f;
            trail.lifeJitter = 0.5f;
            trail.sizeJitter = 0.2f;
            trail.saturationChangeOverLife = -0.5f;
            trail.life = 0.5f;
            trail.randomXOffset = 5f;
            trail.randomAngleDegrees = 20f;
            trail.yOffset = -trail.length * 0.25f;
            Particles.stream(trail, 1, 60f, -1f, emitter -> !Utils.wasProjectileRemoved(missile) && !missile.isFizzling());

            missile.getEngineStats().getMaxTurnRate().setBaseValue(0f);
            missile.getEngineStats().getTurnAcceleration().setBaseValue(0f);
            missile.getEngineStats().getMaxTurnRate().modifyFlat(id, turnRate);
            missile.getEngineStats().getTurnAcceleration().modifyFlat(id, turnRate * 10f);
            missile.getEngineStats().getMaxSpeed().modifyMult(id, SPEED_MULT);
            missile.setHitpoints(missile.getHitpoints() * (1f + hpBoost));
            missile.setMass(missile.getMass() * 2f);
            missile.setMaxFlightTime(missile.getMaxFlightTime() / SPEED_MULT);

            // Could just use MissileAI, but then would have to set the missile's spec to alwaysAccelerate,
            // which is a global permanent change
            missile.setMissileAI(new LOSMissileAI(missile, 3f));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float weight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.MISSILE, 0.2f, 0.3f);
        if (weight <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(weight, 0f, 0.4f, 1f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        float score = 0f;
        for (var id : fm.getVariant().getFittedWeaponSlots()) {
            var weaponSpec = fm.getVariant().getWeaponSpec(id);
            if (weaponSpec != null) {
                var projSpec = weaponSpec.getProjectileSpec();
                if (projSpec instanceof MissileSpecAPI mSpec) {
                    if (mSpec.getDamage().getBaseDamage() >= MIN_DAMAGE  && "ROCKET".equals(mSpec.getTypeString())) {
                        score += switch (weaponSpec.getSize()) {
                            case SMALL -> 1f;
                            case MEDIUM -> 2f;
                            case LARGE -> 4f;
                        };
                    }
                }
            }
        }
        return Math.min(3f, score/3f);
    }
}
