package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.config.Settings;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.ParticleBurstEmitter;
import shipmastery.fx.TrailEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class HEFExplosion extends ShipSystemEffect {

    static final Color trailColor = new Color(1f, 0.7f, 0.7f, 1f);

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.HEFExplosion).params(getSystemName());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.HEFExplosionPost, 0f, new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR}, Utils.asPercent(getStrengthForPlayer()), Utils.asPercent(0.75f));
        tooltip.addPara(Strings.Descriptions.HEFExplosionPost2, 0f);
    }

    @Override
    public void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship == null || ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(HEFExplosionScript.class)) {
            ship.addListener(new HEFExplosionScript(ship, id, getStrength(ship)));
        }
    }

    @Override
    public void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {
        ship.removeListenerOfClass(HEFExplosionScript.class);
    }

    @Override
    public String getSystemSpecId() {
        return "highenergyfocus";
    }

    static class HEFExplosionScript extends BaseShipSystemListener implements DamageDealtModifier, ProjectileCreatedListener {
        final ShipAPI ship;
        final Set<WeaponAPI> largeEnergyWeapons = new HashSet<>();
        final String id;
        final float damageFrac;

        HEFExplosionScript(ShipAPI ship, String id, float damageFrac) {
            this.ship = ship;
            this.id = id;
            this.damageFrac = damageFrac;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                if (weapon.getSlot() != null
                        && WeaponAPI.WeaponType.ENERGY.equals(weapon.getSlot().getWeaponType())
                        && WeaponAPI.WeaponSize.LARGE.equals(weapon.getSlot().getSlotSize())) {
                    largeEnergyWeapons.add(weapon);
                }
            }
        }

        @Override
        public void advanceWhileOn(float amount) {
            Utils.maintainStatusForPlayerShip(ship,
                    id,
                    "graphics/icons/hullsys/high_energy_focus.png",
                    Strings.Descriptions.HEFExplosionTitle,
                    Strings.Descriptions.HEFExplosionDesc1,
                    false);
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, final DamageAPI damage,
                                        final Vector2f pt, boolean shieldHit) {
            if (!(param instanceof DamagingProjectileAPI proj)) return null;
            if (proj.getCustomData() == null || !proj.getCustomData().containsKey(id)) return null;

            final float damageAmount = DamageType.FRAGMENTATION.equals(proj.getDamageType())
                    ? damage.getDamage() / 4f
                    : damage.getDamage();
            final float radius = (float) Math.sqrt(damageAmount) * 5f;
            CombatDeferredActionPlugin.performLater(() -> {
                // Only spawn the explosion if the projectile is destroyed
                // isFading is set after modifyDamageDealt call, so need to do this check
                // later
                if (!Utils.wasProjectileRemoved(proj)) return;
                proj.getCustomData().remove(id);
                float[] explosionColorComps = new float[4];
                trailColor.getComponents(explosionColorComps);
                explosionColorComps[3] = damageAmount / 10000f;
                Color explosionColor = new Color(explosionColorComps[0], explosionColorComps[1], explosionColorComps[2], explosionColorComps[3]);
                DamagingExplosionSpec spec = new DamagingExplosionSpec(
                        0.1f,
                        radius,
                        radius / 2f,
                        damageAmount * damageFrac,
                        damageAmount * damageFrac / 2f,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        1f,
                        4f,
                        0.5f,
                        0,
                        explosionColor,
                        explosionColor
                );
                spec.setDamageType(DamageType.HIGH_EXPLOSIVE);
                spec.setUseDetailedExplosion(false);
                Global.getCombatEngine().spawnDamagingExplosion(spec, ship, pt).getDamage().setFluxComponent(1f);

                ParticleBurstEmitter burst = new ParticleBurstEmitter(pt);
                burst.size = 7f;
                burst.sizeJitter = 0.5f;
                burst.lifeJitter = 0.5f;
                burst.radiusJitter = 1f;
                burst.radius = radius;
                burst.alpha = 0.6f;
                burst.alphaJitter = 0.4f;
                burst.color = trailColor;
                burst.life = radius / 250f;
                Particles.burst(burst, (int) radius);
            }, 0f);
            return null;
        }

        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!ship.getSystem().isOn()) return;
            if (!largeEnergyWeapons.contains(proj.getWeapon())) return;
            proj.setCustomData(id, true);
            TrailEmitter trail = new TrailEmitter(proj);
            trail.color = trailColor;
            trail.length = proj.getMoveSpeed() * 0.05f;
            trail.width = 10f;
            trail.lifeJitter = 0.25f;
            trail.sizeJitter = 0.25f;
            trail.life = 0.2f;
            trail.yOffset = -trail.length * 0.25f;
            Particles.stream(trail, 1, 60f, -1f, emitter -> !Utils.wasProjectileRemoved(emitter.getProj()));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        Utils.WeaponSlotCount count = Utils.countWeaponSlotsStrict(spec);
        if (count.le <= 0) return null;
        return super.getSelectionWeight(spec);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return !fm.isFlagship() ? 0f : 10f*super.getNPCWeight(fm);
    }
}
