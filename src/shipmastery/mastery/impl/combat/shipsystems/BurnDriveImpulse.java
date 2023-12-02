package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.OrionDeviceStats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.ShapedChargeEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;

import java.awt.Color;

public class BurnDriveImpulse extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return null;
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"burndrive".equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(BurnDriveImpulseScript.class)) {
            ship.addListener(new BurnDriveImpulseScript(ship, getStrength(ship)));
        }
    }

    static class BurnDriveImpulseScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float speed;
        float elapsedWhileActive = 0f;

        BurnDriveImpulseScript(ShipAPI ship, float speed) {
            this.ship = ship;
            this.speed = speed;
        }

        @Override
        public void advanceWhileOn(float amount) {
            elapsedWhileActive += amount;
        }

        @Override
        public void onDeactivate() {
            if (elapsedWhileActive < ship.getSystem().getChargeUpDur() + ship.getSystem().getChargeActiveDur() + ship.getSystem().getChargeDownDur() - 0.25f) {
                for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                    // node location obfuscated, have to calculate manually
                    Vector2f nodeLocation = Misc.rotateAroundOrigin(Vector2f.sub(engine.getLocation(), ship.getLocation(), null), -ship.getFacing());

                    // This is so hacky, might as well write an impulse generator from scratch...
                    WeaponSlotAPI tempSlot =
                            Global.getSettings().createWeaponSlot("temp", WeaponAPI.WeaponType.BALLISTIC,
                                                                  WeaponAPI.WeaponSize.LARGE, "HIDDEN", "temp",
                                                                  nodeLocation, engine.getEngineSlot().getAngle(), 0f);
                    CustomOrionDevice impulse = new CustomOrionDevice();
                    impulse.init();
                    impulse.spawnBomb(ship, tempSlot);

                    ShapedChargeEmitter emitter = new ShapedChargeEmitter(ship);
                    emitter.lifeRandomness = 0.25f;
                    emitter.life = 1.5f;
                    emitter.fadeInFrac = 0.01f;
                    emitter.fadeOutFrac = 0.6f;
                    emitter.angleSpread = 30f;
                    emitter.minVelocity = 50f;
                    emitter.maxVelocity = 400f;
                    emitter.size = 120f;
                    emitter.sizeRandomness = 0.25f;
                    emitter.color = new Color(255, 125, 0, 20);
                    emitter.angle = engine.getEngineSlot().getAngle();
                    emitter.saturationShiftOverLife = -1f;
                    emitter.offset = nodeLocation;
                    Particles.burst(emitter, 50);
                }
            }
            elapsedWhileActive = 0f;


        }
    }

    static class CustomOrionDevice extends OrionDeviceStats {
        void init() {
            p.bombLiveTime = 0f;
            p.bombFadeInTime = 0f;
            p.shapedExplosionEndSizeMax = 3f;
            p.shapedExplosionArc = 10f;
            p.shapedExplosionMinParticleSize = 50f;
            p.shapedExplosionMaxParticleSize = 80f;
            p.shapedExplosionNumParticles = 0;
            p.impactAccel = 10000f;
            p.impactRateMult = 1f;
            p.bombWeaponId = "sms_custom_od_launcher";
        }
    }
}
