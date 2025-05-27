package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.opengl.GL11;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.BurstEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class BurnDriveMissileBoost extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BurnDriveMissileBoost).params(getSystemName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public String getSystemSpecId() {
        return "burndrive";
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Float mult = super.getSelectionWeight(spec);
        if (mult == null) return null;
        // Must have at least one missile weapon
        Utils.WeaponSlotCount count = Utils.countWeaponSlots(spec);
        float weight = count.computeWeaponWeight(WeaponAPI.WeaponType.MISSILE, 0.2f, 0.3f);
        return weight == 0 ? null : mult * Utils.getSelectionWeightScaledByValueIncreasing(weight, 0.2f, 0.3f, 0.8f);
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(BurnDriveMissileBoostScript.class)) {
            ship.addListener(new BurnDriveMissileBoostScript(ship, getStrength(ship)));
        }
    }

    class BurnDriveMissileBoostScript extends BaseShipSystemListener implements AdvanceableListener {

        final ShipAPI ship;
        final float mult;
        final Map<BurstEmitter, WeaponAPI> emitters = new HashMap<>();
        final IntervalUtil burstInterval = new IntervalUtil(0.25f, 0.35f);
        final float particleLife = 1f;
        float timeSinceDeactivated = 0f;
        boolean active = false;

        BurnDriveMissileBoostScript(ShipAPI ship, float mult) {
            this.ship = ship;
            this.mult = mult;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (WeaponAPI.WeaponType.MISSILE.equals(weapon.getType())) {
                    if (weapon.getSprite() == null) continue;
                    BurstEmitter emitter = new BurstEmitter(weapon.getSprite(), Color.RED, 4, 0f, particleLife);
                    emitter.layer = CombatEngineLayers.BELOW_PHASED_SHIPS_LAYER;
                    emitter.blendDestFactor = GL11.GL_ONE;
                    emitter.color = new Color(1f, 0.2f, 0f, 1f);
                    emitter.widthGrowth = 15f;
                    emitter.alphaMult = 0.2f;
                    emitter.fadeInFrac = 0.5f;
                    emitter.fadeOutFrac = 0.5f;
                    emitter.enableDynamicAnchoring();
                    emitters.put(emitter, weapon);
                }
            }
        }

        @Override
        public void onActivate() {
            active = true;
        }

        @Override
        public void advanceWhileOn(float amount) {
            float modifiedMult = mult * ship.getSystem().getEffectLevel();
            ship.getMutableStats().getMissileMaxSpeedBonus().modifyPercent(id, 100f * modifiedMult);
            ship.getMutableStats().getMissileAccelerationBonus().modifyPercent(id, 100f * modifiedMult);
            ship.getMutableStats().getMissileHealthBonus().modifyPercent(id, 100f * modifiedMult);
            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    getSystemSpec().getIconSpriteName(),
                    Strings.Descriptions.BurnDriveMissileBoostTitle,
                    String.format(Strings.Descriptions.BurnDriveMissileBoostDesc1, Utils.asPercentNoDecimal(modifiedMult)),
                    false);
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getMissileMaxSpeedBonus().unmodify(id);
            ship.getMutableStats().getMissileAccelerationBonus().unmodify(id);
            ship.getMutableStats().getMissileHealthBonus().unmodify(id);
            timeSinceDeactivated = 0f;
            active = false;
        }

        @Override
        public void advance(float amount) {
            timeSinceDeactivated += amount;
            if (active || timeSinceDeactivated < particleLife) {
                burstInterval.advance(amount);
                boolean burst = burstInterval.intervalElapsed();
                for (Map.Entry<BurstEmitter, WeaponAPI> entry : emitters.entrySet()) {
                    BurstEmitter emitter = entry.getKey();
                    WeaponAPI weapon = entry.getValue();
                    emitter.xDir = weapon.getCurrAngle() - 90f;
                    emitter.location = weapon.getLocation();
                    if (burst && active) {
                        Particles.burst(emitter, 4);
                    }
                }
            }
        }
    }
}
