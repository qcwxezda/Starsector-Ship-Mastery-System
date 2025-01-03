package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
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
import shipmastery.util.EngineUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MissileRegenOnKill extends BaseMasteryEffect {
    public static final float DAMAGE_MULT = 0.75f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .init(Strings.Descriptions.MissileRegenOnKill)
                .params(Misc.getHullSizeStr(selectedModule.getHullSize()), Utils.asPercent(getStrength(selectedModule)), Utils.asPercent(1f - DAMAGE_MULT))
                .colors(Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR);
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
            if (EngineUtils.shipIsOwnedBy(source, ship)) {
                if (target.isFighter() || Utils.hullSizeToInt(target.getHullSize()) < Utils.hullSizeToInt(ship.getHullSize())) return;

                for (WeaponAPI weapon : missileWeapons) {
                    float reloadAmount = weapon.getSpec().getMaxAmmo() * strength;
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

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // Small count as 1, medium as 2, large as 4
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = wsc.sm + wsc.mm * 2f  + wsc.lm * 4f;
        if (count == 0) return null;
        return Utils.getSelectionWeightScaledByValue(count, 5f, false);
    }
}
