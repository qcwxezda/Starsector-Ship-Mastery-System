package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.ShipSystemListener;
import shipmastery.deferred.Action;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.ReflectionUtils;

import java.awt.*;

public class FortressShieldReflection extends BaseMasteryEffect implements ShipSystemListener, DamageTakenModifier {

    float activatedTime = 0f;

    @Override
    public void onActivate(ShipAPI ship) {
        activatedTime = 0f;
    }

    @Override
    public void onDeactivate(ShipAPI ship) {
        activatedTime = 0f;
    }

    @Override
    public void onFullyActivate(ShipAPI ship) {}

    @Override
    public void onFullyDeactivate(ShipAPI ship) {
    }

    @Override
    public void onGainedAmmo(ShipAPI ship) {}

    @Override
    public void onFullyCharged(ShipAPI ship) {}

    @Override
    public void advanceWhileOn(ShipAPI ship, float amount) {
        activatedTime += amount;
        ship.setJitterUnder(ship, Color.WHITE, Math.min(activatedTime, getMaxTime() - activatedTime), 5, 20f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getSystem() == null || !"fortressshield".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(FortressShieldReflection.class)) {
            ship.addListener(this);
        }
        ship.setJitterShields(true);
        ship.setCircularJitter(true);
        ship.getSystem().setFluxPerUse(2000f);
    }

    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.init("Placeholder text!");
    }

    @Override
    public String modifyDamageTaken(Object param, final CombatEntityAPI target, DamageAPI damage, Vector2f pt,
                                    boolean shieldHit) {
        if (!shieldHit || activatedTime > getMaxTime()) return null;
        if (!(param instanceof DamagingProjectileAPI) || param instanceof MissileAPI) return null;
        if (!(target instanceof ShipAPI)) return null;

        DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
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
