package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.deferred.Action;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.*;

public class PilumSalamanderBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.PilumSalamanderBoost)
                .params(
                        ((MissileSpecAPI) Global.getSettings().getWeaponSpec("pilum").getProjectileSpec()).getHullSpec().getHullName(),
                        ((MissileSpecAPI) Global.getSettings().getWeaponSpec("heatseeker").getProjectileSpec()).getHullSpec().getHullName(),
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

    static class PilumSalamanderBoostScript implements DamageDealtModifier, AdvanceableListener {
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
                if (projSpec instanceof MissileSpecAPI) {
                    MissileSpecAPI missileSpec = (MissileSpecAPI) projSpec;
                    if (isPilumOrSalamander(missileSpec)) {
                        eligibleWeapons.add(weapon);
                    }
                }
            }
        }


        @Override
        public void advance(float amount) {
            boolean isFiring = false;
            for (WeaponAPI weapon : eligibleWeapons) {
                if (weapon.getChargeLevel() >= 0.999f) {
                    isFiring = true;
                }
            }
            if (isFiring) {
                Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), 2f*ship.getCollisionRadius(), 2f*ship.getCollisionRadius());
                while (itr.hasNext()) {
                    Object o = itr.next();
                    if (!(o instanceof MissileAPI)) continue;
                    MissileAPI missile = (MissileAPI) o;
                    if (!eligibleWeapons.contains(missile.getWeapon())) continue;
                    if (missile.getCustomData() != null && missile.getCustomData().containsKey(id)) continue;
                    missile.setCustomData(id, true);
                    missile.setHitpoints(missile.getHitpoints() * (1f + hpIncrease));
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
            if (!(param instanceof MissileAPI)) return null;
            MissileAPI missile = (MissileAPI) param;
            if (!eligibleWeapons.contains(missile.getWeapon())) return null;

            int numArcs = (int) MathUtils.randBetween(5f, 11f);
            for (int i = 0; i < numArcs; i++) {
                float theta = MathUtils.randBetween(i * 360f / numArcs, (i+1)*360f / numArcs);
                Vector2f endpoint = Misc.getUnitVectorAtDegreeAngle(theta);
                endpoint.scale(MathUtils.randBetween(damageRadius / 3f, damageRadius));
                Vector2f.add(endpoint, pt, endpoint);

                Global.getCombatEngine().spawnEmpArcVisual(
                        pt,
                        null,
                        endpoint,
                        null,
                        10f,
                        new Color(0, 200, 255, 200),
                        new Color(255, 255, 255, 200)).setSingleFlickerMode();
            }
            CombatDeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
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
                }
            }, 0f);
            return null;
        }
    }
}
