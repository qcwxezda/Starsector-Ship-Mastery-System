package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.CombatEngine;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import particleengine.Utils;
import shipmastery.combat.listeners.ShipSystemListener;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.graphics.FortressShieldDeflectionEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

import java.awt.*;

public class FortressShieldDeflection extends BaseMasteryEffect implements ShipSystemListener, DamageTakenModifier {

    float activatedTime = 0f;
    boolean isActive = false;
    FortressShieldDeflectionEmitter emitter;

    @Override
    public void onActivate(final ShipAPI ship) {
        activatedTime = 0f;
        isActive = true;
    }

    @Override
    public void onDeactivate(ShipAPI ship) {

    }

    @Override
    public void onFullyActivate(ShipAPI ship) {

    }

    @Override
    public void onFullyDeactivate(ShipAPI ship) {
        activatedTime = 0f;
        isActive = false;
    }

    @Override
    public void onGainedAmmo(ShipAPI ship) {}

    @Override
    public void onFullyCharged(ShipAPI ship) {}

    @Override
    public void advanceWhileOn(ShipAPI ship, float amount) {
        activatedTime += amount;
        if (activatedTime <= getMaxTime() && isActive) {
            Particles.burst(emitter, (int) (2 + ship.getShield().getActiveArc() * amount * 10f));
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getSystem() == null || !"fortressshield".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(FortressShieldDeflection.class)) {
            ship.addListener(this);
        }
        emitter = new FortressShieldDeflectionEmitter(ship);
        emitter.enableDynamicAnchoring();
        ship.getSystem().setFluxPerUse(1000f);
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init("Placeholder text!");
    }

    @Override
    public String modifyDamageTaken(Object param, final CombatEntityAPI target, DamageAPI damage, Vector2f pt,
                                    boolean shieldHit) {
        if (!shieldHit || !isActive || activatedTime > getMaxTime()) return null;
        if (!(param instanceof DamagingProjectileAPI) || param instanceof MissileAPI) return null;
        if (!(target instanceof ShipAPI)) return null;

        // Sabots are missiles???

        final DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
        if (proj.getWeapon() == null) return null;

        float newFacing = proj.getFacing() + 180f;
        Vector2f shieldCenter = ((ShipAPI) target).getShieldCenterEvenIfNoShield();
        Vector2f towardCenter = new Vector2f(pt.x - shieldCenter.x, pt.y - shieldCenter.y);
        if (towardCenter.lengthSquared() > 0f) {
            towardCenter.normalise();
            Vector2f v = proj.getVelocity();
            float dot = Vector2f.dot(towardCenter, v);
            Vector2f parallel = new Vector2f(towardCenter.x * dot, towardCenter.y * dot);
            Vector2f reflect = new Vector2f(-v.x + 2f*(v.x - parallel.x), -v.y + 2*(v.y - parallel.y));
            newFacing = Misc.getAngleInDegrees(reflect);
        }

        DamagingProjectileAPI newProj =
                (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(
                        proj.getSource(),
                        proj.getWeapon(),
                        proj.getWeapon().getId(),
                        proj.getLocation(),
                        newFacing,
                        null);
        newProj.setSource((ShipAPI) target);
        newProj.setOwner(target.getOwner());
        return null;
    }

    float getMaxTime() {
        return 5f * getStrength();
    }
}
