package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.fx.ShieldOutlineEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Iterator;

public class ShieldDeflection extends BaseMasteryEffect {
    public static final float DAMAGE_TAKEN_MULT = 0.5f;
    public static final float UNFOLD_RATE_MULT = 0.5f;
    public static final float[] TIME_MULT = new float[] {0.375f, 0.5f, 0.75f, 1f};

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        stats.getShieldUnfoldRateMult().modifyMult(id, UNFOLD_RATE_MULT);
        if (ship != null
                && ship.getShield() != null
                && ship.getShield().getType() != ShieldAPI.ShieldType.PHASE
                && ship.getShield().getType() != ShieldAPI.ShieldType.NONE
                && !ship.hasListenerOfClass(ShieldDeflectionScript.class)) {
            ship.addListener(new ShieldDeflectionScript(ship, getMaxTime(ship)));
        }
    }
    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        stats.getShieldUpkeepMult().unmodify(id);
        stats.getShieldUnfoldRateMult().unmodify(id);
        ship.removeListenerOfClass(ShieldDeflectionScript.class);
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.ShieldDeflection).params(
                Utils.asFloatOneDecimal(getMaxTime(selectedModule)),
                Utils.absValueAsPercent(1f - UNFOLD_RATE_MULT)).colors(
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Settings.NEGATIVE_HIGHLIGHT_COLOR);
    }

    float getMaxTime(ShipAPI ship) {
        return TIME_MULT[Utils.hullSizeToInt(ship.getHullSize())] * getStrength(ship);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ShieldDeflectionPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Utils.absValueAsPercent(DAMAGE_TAKEN_MULT));
    }

    public static class ShieldDeflectionScript implements AdvanceableListener {
        float activatedTime;
        final float maxTime;
        ShieldOutlineEmitter emitter;
        final ShipAPI ship;

        ShieldDeflectionScript(ShipAPI ship, float maxTime) {
            this.ship = ship;
            this.maxTime = maxTime;
            activatedTime = maxTime;
        }

        @Override
        public void advance(float amount) {
            if (ship == null) {
                return;
            }
            if (ship.getShield().isOff()) {
                activatedTime = 0f;
                emitter = null;
                return;
            }
            if (activatedTime >= maxTime) {
                return;
            }

            Utils.maintainStatusForPlayerShip(ship,
                    this,
                    "graphics/icons/hullsys/fortress_shield.png",
                    Strings.Descriptions.ShieldDeflectionStatusTitle,
                    String.format(Strings.Descriptions.ShieldDeflectionStatusDesc, Utils.asPercentNoDecimal(1f -
                                                                                                                    DAMAGE_TAKEN_MULT)),
                    false);
            activatedTime += amount;
            if (emitter == null) {
                emitter = new ShieldOutlineEmitter(ship);
                emitter.enableDynamicAnchoring();
            }
            Particles.burst(emitter, (int) (2 + ship.getShield().getActiveArc() * amount * 2.5f));
            float gridSize = 2f*ship.getShieldRadiusEvenIfNoShield() + 100f;
            Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), gridSize, gridSize);
            while (itr.hasNext()) {
                Object o = itr.next();
                if (!(o instanceof DamagingProjectileAPI proj)) continue;
                if (proj.getOwner() == ship.getOwner()) continue;
                Vector2f loc = proj.getLocation(), vel = proj.getVelocity();
                Vector2f scaledVel = MathUtils.inDirectionWithLength(vel, Math.max(30f, vel.length() * amount));
                Vector2f nextFrame = new Vector2f(loc.x + scaledVel.x, loc.y + scaledVel.y);
                Vector2f shieldCenter = ship.getShieldCenterEvenIfNoShield();
                Vector2f pt = CollisionUtils.rayCollisionCheckShield(loc, nextFrame, ship.getShield());
                if (pt != null) {
                    float newFacing;
                    Vector2f towardCenter = new Vector2f(pt.x - shieldCenter.x, pt.y - shieldCenter.y);
                    final Vector2f reflect;

                    if (towardCenter.lengthSquared() > 0f) {
                        towardCenter.normalise();
                        float dot = Vector2f.dot(towardCenter, vel);
                        Vector2f parallel = new Vector2f(towardCenter.x * dot, towardCenter.y * dot);
                        reflect = new Vector2f(-vel.x + 2f * (vel.x - parallel.x), -vel.y + 2 * (vel.y - parallel.y));
                        newFacing = Misc.getAngleInDegrees(reflect);
                    }
                    else {
                        reflect = new Vector2f(1f, 0f);
                        newFacing = 0f;
                    }

                    Vector2f tailEnd = proj.getTailEnd();
                    if (tailEnd != null) {
                        float length = MathUtils.dist(loc, tailEnd);
                        Vector2f scaledReflect = new Vector2f(reflect);
                        MathUtils.safeNormalize(scaledReflect).scale(length);
                        proj.getTailEnd().set(pt);
                        proj.getLocation().set(new Vector2f(pt.x + scaledReflect.x, pt.y + scaledReflect.y));
                    }
                    else {
                        Vector2f scaledReflect = new Vector2f(reflect);
                        MathUtils.safeNormalize(scaledReflect).scale(scaledVel.length());
                        proj.getLocation().set(pt.x + scaledReflect.x, pt.y + scaledReflect.y);
                    }

                    proj.setFacing(newFacing);
                    proj.getVelocity().set(reflect);
                    proj.setOwner(100);
                    proj.setSource(ship);

                    Global.getCombatEngine().applyDamage(
                            proj,
                            ship,
                            pt,
                            proj.getDamageAmount() * DAMAGE_TAKEN_MULT,
                            proj.getDamageType(),
                            proj.getEmpAmount(),
                            false,
                            false,
                            proj.getSource(),
                            true);
                    if (proj.getProjectileSpec() != null) {
                        Color glowColor = proj.getProjectileSpec().getGlowColor();
                        float glowRadius = proj.getProjectileSpec().getHitGlowRadius();
                        if (glowColor != null) {
                            Global.getCombatEngine()
                                  .addHitParticle(pt, ship.getVelocity(), 2f * glowRadius, 0.3f, glowColor);
                            Global.getCombatEngine()
                                  .addHitParticle(pt, ship.getVelocity(), 0.5f * glowRadius, 0.6f, glowColor);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.hasShield(spec)) return null;
        if (!ShipAPI.HullSize.CAPITAL_SHIP.equals(spec.getHullSize())) return 0f;
        return Utils.getSelectionWeightScaledByValueDecreasing(spec.getBaseShieldFluxPerDamageAbsorbed(), 0.4f, 0.6f, 1f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return !fm.isFlagship() ? 0f : 3f*super.getNPCWeight(fm);
    }
}
