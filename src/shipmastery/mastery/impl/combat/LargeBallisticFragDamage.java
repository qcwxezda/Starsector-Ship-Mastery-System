package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.deferred.Action;
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
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.LargeBallisticFragDamage).params(
                Utils.asInt(getStrength(selectedModule)));
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
        final float damageAmount;
        final Set<WeaponAPI> largeBallistics = new HashSet<>();
        final Color burstColor = new Color(200, 200, 180, 255);

        LargeBallisticFragDamageScript(ShipAPI ship, float damageAmount) {
            this.ship = ship;
            this.damageAmount = damageAmount;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (WeaponAPI.WeaponSize.LARGE.equals(weapon.getSize()) && WeaponAPI.WeaponType.BALLISTIC.equals(weapon.getType())) {
                    largeBallistics.add(weapon);
                }
            }
        }

        @Override
        public String modifyDamageDealt(Object param, final CombatEntityAPI target, DamageAPI damage,
                                        final Vector2f pt, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI)) return null;
            final DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
            if (proj.isFading()) return null;
            if (!largeBallistics.contains(proj.getWeapon())) return null;
            if (DamageType.FRAGMENTATION.equals(damage.getType())) return null;

            // Do this later so that it hits the stripped armor
            CombatDeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    Global.getCombatEngine().applyDamage(
                            proj,
                            target,
                            pt,
                            damageAmount,
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
                    emitter.radius = 30f;
                    Particles.burst(emitter, 30);
                }
            }, 0f);

            // Doesn't modify the damage dealt, just applies its own damage
            return null;
        }
    }
}
