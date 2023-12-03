package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.OrionDeviceStats;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.GlowEmitter;
import shipmastery.fx.ShapedChargeEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

import java.awt.Color;

public class BurnDriveImpulse extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BurnDriveImpulse).params(selectedModule.getSystem().getDisplayName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BurnDriveImpulsePost, 0f);
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
        final float acceleration;
        float elapsedWhileActive = 0f;

        BurnDriveImpulseScript(ShipAPI ship, float acceleration) {
            this.ship = ship;
            this.acceleration = acceleration;
        }

        @Override
        public void advanceWhileOn(float amount) {
            elapsedWhileActive += amount;
        }

        @Override
        public void onDeactivate() {
            if (elapsedWhileActive < ship.getSystem().getChargeUpDur() + ship.getSystem().getChargeActiveDur() + ship.getSystem().getChargeDownDur() - 0.25f) {
                CustomOrionDevice impulse = new CustomOrionDevice();
                int numEngines = ship.getEngineController().getShipEngines().size();
                impulse.init(acceleration, numEngines);
                Vector2f averageEngineLoc = new Vector2f();
                for (ShipEngineControllerAPI.ShipEngineAPI engine : ship.getEngineController().getShipEngines()) {
                    if (engine.isDisabled()) continue;
                    // node location obfuscated, have to calculate manually
                    Vector2f nodeLocation = Misc.rotateAroundOrigin(Vector2f.sub(engine.getLocation(), ship.getLocation(), null), -ship.getFacing());

                    // This is so hacky, might as well write an impulse generator from scratch...
                    WeaponSlotAPI tempSlot =
                            Global.getSettings().createWeaponSlot("temp", WeaponAPI.WeaponType.BALLISTIC,
                                                                  WeaponAPI.WeaponSize.LARGE, "HIDDEN", "temp",
                                                                  nodeLocation, engine.getEngineSlot().getAngle(), 0f);

                    impulse.spawnBomb(ship, tempSlot);

                    ShapedChargeEmitter emitter = new ShapedChargeEmitter(ship);
                    emitter.lifeRandomness = 0.25f;
                    emitter.life = 2f;
                    emitter.fadeInFrac = 0.01f;
                    emitter.fadeOutFrac = 0.8f;
                    emitter.angleSpread = 45f;
                    emitter.minVelocity = 50f;
                    emitter.maxVelocity = 250f;
                    emitter.size = 120f;
                    emitter.sizeRandomness = 0.25f;
                    emitter.color = new Color(255, 125, 0, 30);
                    emitter.angle = engine.getEngineSlot().getAngle();
                    emitter.saturationShiftOverLife = -1f;
                    emitter.offset = nodeLocation;
                    Particles.burst(emitter, 50);
                    Vector2f.add(averageEngineLoc, engine.getLocation(), averageEngineLoc);
                }
                averageEngineLoc.x /= numEngines;
                averageEngineLoc.y /= numEngines;
                GlowEmitter glowEmitter = new GlowEmitter(averageEngineLoc);
                glowEmitter.color = new Color(255, 200, 100, 60);
                glowEmitter.fadeInFrac = 0f;
                glowEmitter.fadeOutFrac = 1f;
                glowEmitter.startSize = 800f;
                glowEmitter.endSize = 800f;
                glowEmitter.maxSize = 800f;
                glowEmitter.life = 1.5f;
                Particles.burst(glowEmitter, 1);
            }
            elapsedWhileActive = 0f;


        }
    }

    static class CustomOrionDevice extends OrionDeviceStats {
        void init(float acceleration, int numEngines) {
            p.bombLiveTime = 0f;
            p.bombFadeInTime = 0f;
            p.shapedExplosionNumParticles = 0;
            p.impactAccel = acceleration / numEngines;
            p.impactRateMult = 1f;
            p.bombWeaponId = "sms_custom_od_launcher";
        }
    }
}
