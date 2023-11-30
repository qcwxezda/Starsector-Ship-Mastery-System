package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import particleengine.Particles;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.fx.JitterEmitter;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class EnergyMineConversion extends ShipSystemEffect {

    static final float EFFECT_RADIUS = 400f;
    static final int NUM_ARCS = 10;
    static final float DAMAGE_FRAC = 0.5f;
    static final float ARC_DAMAGE = 100f;
    static final float ARC_EMP_DAMAGE = 500f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EnergyMineConversion).params(systemName);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"mine_strike".equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(EnergyMineConversionScript.class)) {
            ship.addListener(new EnergyMineConversionScript(ship, id));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.EnergyMineConversionPost,
                0f, new Color[] {Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor()},
                "" + Utils.asPercent(1f - DAMAGE_FRAC), "" + NUM_ARCS, "" + (int) ARC_DAMAGE, "" + (int) ARC_EMP_DAMAGE);
    }

    public static class EnergyMineConversionScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final String id;
        final CombatEngineAPI engine;

        EnergyMineConversionScript(ShipAPI ship, String id) {
            this.ship = ship;
            this.id = id;
            engine = Global.getCombatEngine();
        }

        @Override
        public void onFullyActivate() {
            // Start from the end of the projectile list -- the newly made mine is very likely to be at the
            // end of this list, so this for loop is actually O(1)-ish
            ArrayList<DamagingProjectileAPI> projectiles = (ArrayList<DamagingProjectileAPI>) engine.getProjectiles();
            for (int i = projectiles.size() - 1; i >= 0; i--) {
                DamagingProjectileAPI proj = projectiles.get(i);
                if (ship == proj.getSource() && "minelayer_mine_heavy".equals(proj.getProjectileSpecId())) {
                    MissileAPI mine = (MissileAPI) engine.spawnProjectile(
                            ship,
                            null,
                            "sms_energyminelayer",
                            proj.getLocation(),
                            proj.getFacing(),
                            null);
                    mine.setDamageAmount(proj.getBaseDamageAmount() * DAMAGE_FRAC);
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
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static class EnergyMineExplosionScript implements ProximityExplosionEffect {
        @Override
        public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI mine) {
            CombatEngineAPI engine = Global.getCombatEngine();
            Iterator<Object> grid = engine.getShipGrid().getCheckIterator(mine.getLocation(), 2f* EFFECT_RADIUS, 2f*
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
            for (int i = 0; i < NUM_ARCS; i++) {
                CombatEntityAPI target = picker.pick();
                boolean pierced = false;
                if (target instanceof ShipAPI) {
                    ShipAPI ship = ((ShipAPI) target);
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
                            ARC_DAMAGE,
                            ARC_EMP_DAMAGE,
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
                            150f,
                            1000f,
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
