package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.combat.listeners.FlagshipListener;
import shipmastery.graphics.ShieldDeflectionEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.Iterator;

public class ShieldDeflection extends BaseMasteryEffect implements AdvanceableListener, FlagshipListener {

    static final DecimalFormat numberFormat = new DecimalFormat("0.#");
    static final float damageTakenMult = 0.5f;
    static final float upkeepMult = 1.5f;
    static final float unfoldRateMult = 0.5f;
    float activatedTime = 0f;
    ShieldDeflectionEmitter emitter;
    ShipAPI ship;
    boolean enabled = false;
    String id = "";


    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.hasListenerOfClass(ShieldDeflection.class)) {
            ship.addListener(this);
            this.ship = ship;
            emitter = new ShieldDeflectionEmitter(ship);
            emitter.enableDynamicAnchoring();
            this.id = id;
        }
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.ShieldDeflection).params(
                Strings.FLAGSHIP_ONLY,
                numberFormat.format(getMaxTime()),
                Utils.absValueAsPercent(upkeepMult - 1f),
                Utils.absValueAsPercent(1f - unfoldRateMult)).colors(
                        Misc.getBasePlayerColor(),
                        Misc.getHighlightColor(),
                        Misc.getNegativeHighlightColor(),
                        Misc.getNegativeHighlightColor());
    }

    float getMaxTime() {
        return 8f * getStrength();
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ShieldDeflectionPost, 5f, Misc.getHighlightColor(),
                        Utils.absValueAsPercent(damageTakenMult));
    }

    @Override
    public void advance(float amount) {
        if (!enabled) {
            activatedTime = 99999f; // Force turning shields off and back on again if transferring command
            return;
        }
        if (ship.getShield().isOff()) {
            activatedTime = 0f;
            return;
        }
        if (activatedTime > getMaxTime()) {
            return;
        }
        activatedTime += amount;
        Particles.burst(emitter, (int) (2 + ship.getShield().getActiveArc() * amount * 5f));
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

    void applyOrUnapplyPassiveEffects() {
        if (enabled) {

        }
        else {

        }
    }

    @Override
    public void playerFlagshipChanged(ShipAPI from, ShipAPI to) {
        if (to.getOwner() != ship.getOwner()) return;
        enabled = to == ship;
        applyOrUnapplyPassiveEffects();
    }

    @Override
    public void enemyFlagshipChanged(ShipAPI from, ShipAPI to) {
        if (to.getOwner() != ship.getOwner()) return;
        enabled = to == ship;
        applyOrUnapplyPassiveEffects();
    }
}
