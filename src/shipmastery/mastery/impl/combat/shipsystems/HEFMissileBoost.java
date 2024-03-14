package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.opengl.GL11;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.config.Settings;
import shipmastery.fx.BurstEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class HEFMissileBoost extends ShipSystemEffect {
    static final float[] FLUX_PER_SECOND = new float[] {100f, 200f, 300f, 400f};
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.HEFMissileBoost).params(getSystemName(), Utils.asPercent(getStrengthForPlayer()));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.HEFMissileBoostPost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR,
                        Utils.asInt(FLUX_PER_SECOND[Utils.hullSizeToInt(selectedModule.getHullSize())]));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(HEFMissileBoostScript.class)) {
            ship.addListener(new HEFMissileBoostScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "highenergyfocus";
    }

    static class HEFMissileBoostScript extends BaseShipSystemListener implements AdvanceableListener {
        final ShipAPI ship;
        final float mult;
        final String id;
        final Map<BurstEmitter, WeaponAPI> emitters = new HashMap<>();
        final IntervalUtil burstInterval = new IntervalUtil(0.25f, 0.35f);
        final float particleLife = 1f;
        float timeSinceDeactivated = 0f;
        boolean active = false;

        HEFMissileBoostScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
            ship.getSystem().setFluxPerSecond(ship.getSystem().getFluxPerSecond() + FLUX_PER_SECOND[Utils.hullSizeToInt(ship.getHullSize())]);

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (WeaponAPI.WeaponType.MISSILE.equals(weapon.getType())) {
                    BurstEmitter emitter = new BurstEmitter(weapon.getSprite(), new Color(255, 0, 255), 6, 0f, particleLife);
                    emitter.layer = CombatEngineLayers.BELOW_PHASED_SHIPS_LAYER;
                    emitter.blendDestFactor = GL11.GL_ONE;
                    emitter.widthGrowth = 10f;
                    emitter.alphaMult = 0.15f;
                    emitter.fadeInFrac = 0.6f;
                    emitter.fadeOutFrac = 0.4f;
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
        public void onFullyDeactivate() {
            ship.getMutableStats().getMissileHealthBonus().unmodify(id);
            ship.getMutableStats().getMissileMaxSpeedBonus().unmodify(id);
            ship.getMutableStats().getMissileGuidance().unmodify(id);
            ship.getMutableStats().getMissileWeaponDamageMult().unmodify(id);
            timeSinceDeactivated = 0f;
            active = false;
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel() * mult;
            ship.getMutableStats().getMissileHealthBonus().modifyPercent(id, effectLevel * 100f);
            ship.getMutableStats().getMissileMaxSpeedBonus().modifyPercent(id, effectLevel * 100f);
            ship.getMutableStats().getMissileGuidance().modifyPercent(id, effectLevel * 100f);
            ship.getMutableStats().getMissileWeaponDamageMult().modifyPercent(id, effectLevel * 100f);
            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    "graphics/icons/hullsys/high_energy_focus.png",
                    Strings.Descriptions.HEFMissileBoostTitle,
                    String.format(Strings.Descriptions.HEFMissileBoostDesc1, Utils.asPercentNoDecimal(effectLevel)),
                    false);
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
                        Particles.burst(emitter, 6);
                    }
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Float mult = super.getSelectionWeight(spec);
        if (mult == null) return null;
        // Must have at least one missile weapon
        Utils.WeaponSlotCount count = Utils.countWeaponSlots(spec);
        int weightedCount = count.sm + 2*count.mm + 4*count.lm;
        return weightedCount == 0 ? null : mult * Utils.getSelectionWeightScaledByValue(weightedCount, 4f, false);
    }
}
