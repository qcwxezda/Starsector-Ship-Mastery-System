package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.ParticleBurstEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class LargeBallisticFragDamage extends BaseMasteryEffect {

    public static final float DAMAGE_FRAC = 0.5f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.LargeBallisticFragDamage).params(
                Utils.asPercent(DAMAGE_FRAC), Utils.asInt(getStrength(selectedModule)))
                .colors(Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.LargeBallisticFragDamagePost, 0f);
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship != null && !ship.hasListenerOfClass(LargeBallisticFragDamageScript.class)) {
            ship.addListener(new LargeBallisticFragDamageScript(ship, getStrength(ship)));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(LargeBallisticFragDamageScript.class);
    }

    static class LargeBallisticFragDamageScript implements DamageDealtModifier {

        final ShipAPI ship;
        final float maxDamage;
        final Set<WeaponAPI> largeBallistics = new HashSet<>();
        final Color burstColor = new Color(200, 200, 180, 255);

        LargeBallisticFragDamageScript(ShipAPI ship, float maxDamage) {
            this.ship = ship;
            this.maxDamage = maxDamage;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (WeaponAPI.WeaponSize.LARGE.equals(weapon.getSize()) && WeaponAPI.WeaponType.BALLISTIC.equals(weapon.getType())) {
                    largeBallistics.add(weapon);
                }
            }
        }

        @Override
        public String modifyDamageDealt(Object param, final CombatEntityAPI target, DamageAPI damage,
                                        final Vector2f pt, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI proj)) return null;
            if (proj.isFading()) return null;
            if (!largeBallistics.contains(proj.getWeapon())) return null;
            if (DamageType.FRAGMENTATION.equals(damage.getType())) return null;

            final float fragDamage = Math.min(maxDamage, proj.getWeapon().getDamage().getBaseDamage() * DAMAGE_FRAC);

            // Do this later so that it hits the stripped armor
            CombatDeferredActionPlugin.performLater(() -> {
                Global.getCombatEngine().applyDamage(
                        proj,
                        target,
                        pt,
                        fragDamage,
                        DamageType.FRAGMENTATION,
                        0f,
                        false,
                        false,
                        ship,
                        false);
                ParticleBurstEmitter emitter = new ParticleBurstEmitter(pt);
                emitter.color = burstColor;
                emitter.lengthMultiplierOverTime = 2f;
                emitter.radiusJitter = 0.8f;
                emitter.life = 0.35f;
                emitter.lifeJitter = 0.5f;
                emitter.size = 4f;
                emitter.radius = 5f + 2f * (float) Math.sqrt(fragDamage);
                Particles.burst(emitter, 10 + (int) (3f * (float) Math.sqrt(fragDamage)));
            }, 0f);

            // Doesn't modify the damage dealt, just applies its own damage
            return null;
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = wsc.lb;
        if (count <= 0f) return null;
        return 1.6f * Utils.getSelectionWeightScaledByValue(count, 1f, false);
    }
}
