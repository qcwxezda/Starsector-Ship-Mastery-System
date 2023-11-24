package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.fx.ShieldDeflectionEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.Iterator;

public class ShieldDeflection extends BaseMasteryEffect {
    static final float damageTakenMult = 0.5f;
    static final float upkeepMult = 1.5f;
    static final float unfoldRateMult = 0.5f;

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        stats.getShieldUpkeepMult().modifyMult(id, upkeepMult);
        stats.getShieldUnfoldRateMult().modifyMult(id, unfoldRateMult);
        if (ship != null
                && ship.getShield() != null
                && ship.getShield().getType() != ShieldAPI.ShieldType.PHASE
                && ship.getShield().getType() != ShieldAPI.ShieldType.NONE) {
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
                Utils.oneDecimalPlaceFormat.format(getPlayerMaxTime()),
                Utils.absValueAsPercent(upkeepMult - 1f),
                Utils.absValueAsPercent(1f - unfoldRateMult)).colors(
                        Misc.getHighlightColor(),
                        Misc.getNegativeHighlightColor(),
                        Misc.getNegativeHighlightColor());
    }

    float getPlayerMaxTime() {return getStrengthForPlayer();}

    float getMaxTime(ShipAPI ship) {
        return getStrength(ship);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ShieldDeflectionPost, 0f, Misc.getHighlightColor(),
                        Utils.absValueAsPercent(damageTakenMult));
    }

    public static class ShieldDeflectionScript implements AdvanceableListener {
        float activatedTime;
        final float maxTime;
        final ShieldDeflectionEmitter emitter;
        final ShipAPI ship;

        ShieldDeflectionScript(ShipAPI ship, float maxTime) {
            this.ship = ship;
            this.maxTime = maxTime;
            activatedTime = maxTime;
            this.emitter = new ShieldDeflectionEmitter(ship);
            emitter.enableDynamicAnchoring();
        }

        @Override
        public void advance(float amount) {
            if (ship == null) {
                return;
            }
            if (ship.getShield().isOff()) {
                activatedTime = 0f;
                return;
            }
            if (activatedTime >= maxTime) {
                return;
            }

            Global.getCombatEngine().maintainStatusForPlayerShip(
                    this,
                    "graphics/icons/hullsys/fortress_shield.png",
                    Strings.Descriptions.ShieldDeflectionStatusTitle,
                    String.format(Strings.Descriptions.ShieldDeflectionStatusDesc, Utils.asPercent(1f - damageTakenMult)),
                    false);

            activatedTime += amount;
            Particles.burst(emitter, (int) (2 + ship.getShield().getActiveArc() * amount * 2f));
            float gridSize = 2f*ship.getShieldRadiusEvenIfNoShield() + 100f;
            Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), gridSize, gridSize);
            while (itr.hasNext()) {
                Object o = itr.next();
                if (!(o instanceof DamagingProjectileAPI)) continue;
                DamagingProjectileAPI proj = (DamagingProjectileAPI) o;
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
                            ship,
                            ship,
                            pt,
                            proj.getDamageAmount() * damageTakenMult,
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
}
