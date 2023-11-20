package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Emitter;
import particleengine.Particles;
import particleengine.Utils;
import shipmastery.combat.listeners.ShipSystemListener;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

import java.awt.*;

public class FortressShieldReflection extends BaseMasteryEffect implements ShipSystemListener, DamageTakenModifier {

    float activatedTime = 0f;

    @Override
    public void onActivate(final ShipAPI ship) {
        activatedTime = 0f;
        final Emitter test = Particles.initialize(ship.getLocation());
        test.circleOffset(10f, 100f);
        test.radialVelocity(-5f, -10f);
        test.revolutionRate(-30f, 30f);
        test.radialAcceleration(-20f, 20f);
        test.setSyncSize(true);
        test.growthRate(-5f, 5f);
        test.life(10f, 10f);
        test.size(25, 25);
        test.fadeTime(1f, 1f, 1f, 1);
        test.sinusoidalMotionX(100f, 200f, 0.25f, 0.5f, 0f, 0f);
        test.setBlendMode(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
        test.enableDynamicAnchoring();
        Particles.anchorEmitter(test, ship);
        final Emitter test2 = Particles.createCopy(test, Utils.getLoadedSprite("graphics/fx/explosion1.png"), GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
        final Emitter test3 = Particles.createCopy(test, Utils.getLoadedSprite("graphics/fx/explosion2.png"), GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL14.GL_FUNC_ADD);
        Particles.anchorEmitter(test2, ship);
        test2.enableDynamicAnchoring();
        Particles.anchorEmitter(test3, ship);
        test3.enableDynamicAnchoring();
        test3.setLayer(CombatEngineLayers.BELOW_SHIPS_LAYER);

        test.randomHSVA(360, 1, 1, 0);
        Particles.stream(test, 999, 100000, 10);
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
        //ship.addAfterimage(Color.WHITE, )
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getSystem() == null || !"fortressshield".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(FortressShieldReflection.class)) {
            ship.addListener(this);
        }
        ship.setJitterShields(true);
        ship.setCircularJitter(true);
        ship.setShowModuleJitterUnder(true);
        ship.getSystem().setFluxPerUse(2000f);
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
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
