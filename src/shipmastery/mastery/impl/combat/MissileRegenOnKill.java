package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import particleengine.Particles;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.config.Settings;
import shipmastery.fx.OverlayEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MissileRegenOnKill extends BaseMasteryEffect {

    static final float[] AMMO_PER_KILL_MULTIPLIER = new float[] {1f, 2f, 3f, 4f};
    public static final float DAMAGE_MULT = 0.75f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        String frigate = Utils.asPercent(AMMO_PER_KILL_MULTIPLIER[0] * strength);
        String destroyer = Utils.asPercent(AMMO_PER_KILL_MULTIPLIER[1] * strength);
        String cruiser = Utils.asPercent(AMMO_PER_KILL_MULTIPLIER[2] * strength);
        String capital = Utils.asPercent(AMMO_PER_KILL_MULTIPLIER[3] * strength);
        return MasteryDescription
                .init(Strings.Descriptions.MissileRegenOnKill)
                .params(frigate, destroyer, cruiser, capital, Utils.asPercent(1f - DAMAGE_MULT))
                .colors(Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.NEGATIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.MissileRegenOnKillPost, 0f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getMissileWeaponDamageMult().modifyMult(id, DAMAGE_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null) {
            return;
        }
        if (!ship.hasListenerOfClass(MissileRegenOnKillScript.class)) {
            ship.addListener(new MissileRegenOnKillScript(ship, getStrength(ship)));
        }
    }

    static class MissileRegenOnKillScript implements ShipDestroyedListener {
        final ShipAPI ship;
        final float strength;
        final List<WeaponAPI> missileWeapons = new ArrayList<>();

        MissileRegenOnKillScript(ShipAPI ship, float strength) {
            this.ship = ship;
            this.strength = strength;
            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if(WeaponAPI.WeaponType.MISSILE.equals(weapon.getType())) {
                    if (weapon.usesAmmo()) {
                        missileWeapons.add(weapon);
                    }
                }
            }
        }

        @Override
        public void reportShipDestroyed(ShipAPI source, ShipAPI target) {
            if ((source == ship || (source.isFighter() && source.getWing() != null && source.getWing().getSourceShip() == ship)) && !target.isFighter()) {
                float reloadFrac = AMMO_PER_KILL_MULTIPLIER[Utils.hullSizeToInt(target.getHullSize())] * strength;
                for (WeaponAPI weapon : missileWeapons) {
                    float reloadAmount = weapon.getSpec().getMaxAmmo() * reloadFrac;
                    int intReloadAmount = (int) reloadAmount;
                    float frac = reloadAmount - intReloadAmount;
                    if (Misc.random.nextFloat() <= frac) {
                        intReloadAmount++;
                    }
                    weapon.setAmmo(Math.min(weapon.getMaxAmmo(), weapon.getAmmo() + intReloadAmount));
                }
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
