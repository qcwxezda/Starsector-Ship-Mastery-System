package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import particleengine.Particles;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.config.Settings;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Iterator;
import java.util.Map;

public class EnergyMineConversion extends ShipSystemEffect {

    static final float EFFECT_RADIUS = 400f;
    static final int NUM_ARCS = 10;
    static final float DAMAGE_FRAC = 0.6f;
    public static final String PROJ_DAMAGE_KEY = "sms_EnergyMineArcDamage";
    public static final String PROJ_EMP_DAMAGE_KEY = "sms_EnergyMineArcEmp";

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EnergyMineConversion).params(getSystemName());
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(EnergyMineConversionScript.class)) {
            ship.addListener(new EnergyMineConversionScript(ship, getStrength(ship), 5f * getStrength(ship), id));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.EnergyMineConversionPost,
                0f, new Color[] {Settings.NEGATIVE_HIGHLIGHT_COLOR, Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(1f - DAMAGE_FRAC), "" + NUM_ARCS, Utils.asInt(getStrength(selectedModule)), Utils.asInt(5f * getStrength(selectedModule)));
    }

    @Override
    public String getSystemSpecId() {
        return "mine_strike";
    }

    public static class EnergyMineConversionScript implements ProjectileCreatedListener {
        final ShipAPI ship;
        final String id;
        final float damage;
        final float empDamage;
        final CombatEngineAPI engine;

        EnergyMineConversionScript(ShipAPI ship, float damage, float empDamage, String id) {
            this.ship = ship;
            this.id = id;
            this.damage = damage;
            this.empDamage = empDamage;
            engine = Global.getCombatEngine();
        }

        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!"minelayer_mine_heavy".equals(proj.getProjectileSpecId())) return;
            MissileAPI mine = (MissileAPI) engine.spawnProjectile(
                    ship,
                    null,
                    "sms_energyminelayer",
                    proj.getLocation(),
                    proj.getFacing(),
                    null);
            mine.setDamageAmount(proj.getBaseDamageAmount() * DAMAGE_FRAC);
            mine.setCustomData(PROJ_DAMAGE_KEY, damage);
            mine.setCustomData(PROJ_EMP_DAMAGE_KEY, empDamage);
            mine.setMineExplosionRange(EFFECT_RADIUS);
            int numParticles = 20;
            float duration = 0.25f;
            JitterEmitter jitter = new JitterEmitter(mine, mine.getSpriteAPI(), Color.CYAN, 0f, 10f, 0.25f, true, 0.5f, numParticles);
            jitter.enableDynamicAnchoring();
            Particles.stream(jitter, 1, numParticles / duration, duration);
            ((MissileAPI) proj).setArmingTime(10f);
            Global.getCombatEngine().removeEntity(proj);
            // Copied from MineStrikeStats
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(ship, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
            float fadeInTime = 0.5F;
            mine.fadeOutThenIn(fadeInTime);
            float liveTime = 5.0F;
            mine.setFlightTime(mine.getMaxFlightTime() - liveTime);
        }
    }

    @SuppressWarnings("unused")
    public static class EnergyMineExplosionScript implements ProximityExplosionEffect {
        @Override
        public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI mine) {
            CombatEngineAPI engine = Global.getCombatEngine();
            Iterator<Object> grid = engine.getShipGrid().getCheckIterator(mine.getLocation(), 2f*EFFECT_RADIUS, 2f*
                    EFFECT_RADIUS);
            WeightedRandomPicker<CombatEntityAPI> picker = new WeightedRandomPicker<>();
            while (grid.hasNext()) {
                Object o = grid.next();
                if (CollisionUtils.canCollide(o, null, mine.getSource(), false)) {
                    CombatEntityAPI target = (CombatEntityAPI) o;
                    if (MathUtils.dist(target.getLocation(), mine.getLocation()) > EFFECT_RADIUS + target.getCollisionRadius()) {
                        continue;
                    }
                    if (target.getHitpoints() <= 0f) {
                        continue;
                    }
                    picker.add(target, target.getCollisionRadius());
                }
            }
            if (picker.isEmpty()) return;

            Map<String, Object> customData = mine.getCustomData();
            float damage = 0f, empDamage = 0f;
            if (customData != null) {
                damage = (float) customData.get(PROJ_DAMAGE_KEY);
                empDamage = (float) customData.get(PROJ_EMP_DAMAGE_KEY);
            }
            for (int i = 0; i < NUM_ARCS; i++) {
                CombatEntityAPI target = picker.pick();
                boolean pierced = false;
                if (target instanceof ShipAPI ship) {
                    float pierceChance = ship.getHardFluxLevel() - 0.1f;
                    pierceChance *= ship.getMutableStats().getDynamic().getValue(Stats.SHIELD_PIERCED_MULT);
                    if (Math.random() < pierceChance) {
                        pierced = true;
                    }
                }
                if (!pierced) {
                    engine.spawnEmpArc(
                            mine.getSource(),
                            mine.getLocation(),
                            mine,
                            target,
                            DamageType.ENERGY,
                            damage,
                            empDamage,
                            1000000f,
                            "tachyon_lance_emp_impact",
                            40f,
                            new Color(0, 180, 255, 255),
                            new Color(180, 200, 255, 255));
                }
                else {
                    engine.spawnEmpArcPierceShields(
                            mine.getSource(),
                            mine.getLocation(),
                            null,
                            target,
                            DamageType.ENERGY,
                            damage,
                            empDamage,
                            1000000f,
                            "tachyon_lance_emp_impact",
                            40f,
                            new Color(0, 180, 255, 255),
                            new Color(180, 200, 255, 255));
                }
            }
        }
    }
}
