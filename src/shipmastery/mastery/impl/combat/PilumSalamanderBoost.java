package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class PilumSalamanderBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.PilumSalamanderBoost)
                .params(
                        Strings.Descriptions.PilumName,
                        Strings.Descriptions.SalamanderName,
                        Utils.asPercent(strength / 300f),
                        Utils.asInt(strength),
                        Utils.asInt(3f * strength));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(PilumSalamanderBoostScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new PilumSalamanderBoostScript(ship, strength / 300f, strength, 3f * strength, 2f/3f*strength, id));
        }
    }

    static class PilumSalamanderBoostScript implements DamageDealtModifier, ProjectileCreatedListener {
        final ShipAPI ship;
        final float hpIncrease;
        final float damage;
        final float empDamage;
        final float damageRadius;
        final Set<WeaponAPI> eligibleWeapons = new HashSet<>();
        final String id;

        PilumSalamanderBoostScript(ShipAPI ship, float hpIncrease, float damage, float empDamage, float damageRadius, String id) {
            this.ship = ship;
            this.damage = damage;
            this.hpIncrease = hpIncrease;
            this.empDamage = empDamage;
            this.damageRadius = damageRadius;
            this.id = id;

            for (WeaponAPI weapon : ship.getUsableWeapons()) {
                Object projSpec = weapon.getSpec().getProjectileSpec();
                if (projSpec instanceof MissileSpecAPI missileSpec) {
                    if (isPilumOrSalamander(missileSpec)) {
                        eligibleWeapons.add(weapon);
                    }
                }
            }
        }

        boolean isPilumOrSalamander(MissileSpecAPI spec) {
            String id = spec.getHullSpec().getHullId();
            return "pilum_second_stage".equals(id) || "type_1_lrm".equals(id) || "heatseeker_mrm".equals(id);
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, final DamageAPI damage,
                                        final Vector2f pt, boolean shieldHit) {
            if (!(param instanceof MissileAPI missile)) return null;
            if (!eligibleWeapons.contains(missile.getWeapon())) return null;

            int numArcs = (int) MathUtils.randBetween(5f, 11f);
            for (int i = 0; i < numArcs; i++) {
                float theta = MathUtils.randBetween(i * 360f / numArcs, (i+1)*360f / numArcs);
                Vector2f endpoint = Misc.getUnitVectorAtDegreeAngle(theta);
                float outRadius = damageRadius;
                float inRadius = damageRadius / 3f;
                float r = (float) Math.sqrt(Misc.random.nextFloat() * (outRadius*outRadius - inRadius*inRadius) + inRadius*inRadius);
                endpoint.scale(r);
                Vector2f.add(endpoint, pt, endpoint);
                Global.getCombatEngine().spawnEmpArcVisual(
                        pt,
                        null,
                        endpoint,
                        null,
                        8f,
                        new Color(125,125,100,255),
                        new Color(255, 255, 255, 200)).setSingleFlickerMode();
            }
            CombatDeferredActionPlugin.performLater(() -> {
                DamagingExplosionSpec spec = new DamagingExplosionSpec(
                        0.1f,
                        damageRadius,
                        damageRadius / 2f,
                        PilumSalamanderBoostScript.this.damage,
                        PilumSalamanderBoostScript.this.damage/2f,
                        CollisionClass.PROJECTILE_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        0f,
                        0f,
                        0f,
                        0,
                        new Color(0f, 0f, 0f, 0f),
                        null
                );
                spec.setDamageType(DamageType.ENERGY);
                Global.getCombatEngine().spawnDamagingExplosion(spec, ship, pt).getDamage().setFluxComponent(empDamage);
            }, 0f);
            return null;
        }

        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!(proj instanceof MissileAPI missile)) return;
            if (!eligibleWeapons.contains(missile.getWeapon())) return;
            missile.setHitpoints(missile.getHitpoints() * (1f + hpIncrease));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = 0.5f*wsc.sm + 2f*wsc.mm + 4f*wsc.lm;
        if (count < 3f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(count, 2f, 5f, 10f);
    }
}
