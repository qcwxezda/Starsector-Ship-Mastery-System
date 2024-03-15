package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import particleengine.Particles;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.fx.OverlayEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class SystemRegenOnKill extends BaseMasteryEffect {

    static final float[] SECONDS_PER_KILL_MULTIPLIER = new float[] {1f, 2f, 3f, 4f};

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.SystemRegenOnKill);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        float strength = getStrengthForPlayer();
        String frigateTime = Utils.asFloatOneDecimal(SECONDS_PER_KILL_MULTIPLIER[0] * strength);
        String destroyerTime = Utils.asFloatOneDecimal(SECONDS_PER_KILL_MULTIPLIER[1] * strength);
        String cruiserTime = Utils.asFloatOneDecimal(SECONDS_PER_KILL_MULTIPLIER[2] * strength);
        String capitalTime = Utils.asFloatOneDecimal(SECONDS_PER_KILL_MULTIPLIER[3] * strength);
        tooltip.addPara(Strings.Descriptions.SystemRegenOnKillPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, frigateTime, destroyerTime, cruiserTime, capitalTime);
        tooltip.addPara(Strings.Descriptions.SystemRegenOnKillPost2, 0f);
        tooltip.addPara(Strings.Descriptions.SystemRegenOnKillPost3, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null) {
            return;
        }
        if (!ship.hasListenerOfClass(SystemRegenOnKillScript.class)) {
            ship.addListener(new SystemRegenOnKillScript(ship, getStrength(ship)));
        }
    }

    static class SystemRegenOnKillScript implements ShipDestroyedListener {
        final ShipAPI ship;
        final boolean usesAmmo;
        final float strength;

        SystemRegenOnKillScript(ShipAPI ship, float strength) {
            this.ship = ship;
            // Yes, this is the only way to check if the ship system uses ammo without reflection...
            usesAmmo = ship.getSystem().getMaxAmmo() < Integer.MAX_VALUE;
            this.strength = strength;
        }

        @Override
        public void reportShipDestroyed(ShipAPI source, ShipAPI target) {
            if ((source == ship || (source.isFighter() && source.getWing() != null && source.getWing().getSourceShip() == ship)) && !target.isFighter()) {
                int index = Utils.hullSizeToInt(target.getHullSize());
                ShipSystemAPI system = ship.getSystem();
                boolean activated = false;
                if (usesAmmo) {
                    system.setAmmoReloadProgress(system.getAmmoReloadProgress() + system.getAmmoPerSecond() * strength * SECONDS_PER_KILL_MULTIPLIER[index]);
                    activated = true;
                } else {
                    if (!system.isActive()) {
                        system.setCooldownRemaining(Math.max(0f, system.getCooldownRemaining() - strength * SECONDS_PER_KILL_MULTIPLIER[index]));
                        activated = true;
                    }
                }
                if (activated) {
                    Color color = ship.getSpriteAPI().getAverageColor();
                    Color newColor = new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.6f);
                    OverlayEmitter emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 1f);
                    emitter.color = newColor;
                    emitter.enableDynamicAnchoring();
                    Particles.burst(emitter, 1);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getShipSystemId() == null) return null;
        ShipSystemSpecAPI system = Global.getSettings().getShipSystemSpec(spec.getShipSystemId());
        if (system == null) return null;
        float regen = system.getRegen(null);
        if (regen <= 0f) return null;
        return Utils.getSelectionWeightScaledByValue(regen, 0.1f, true);
    }
}
