package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class ReactiveFortressShield extends ShipSystemEffect {

    public static final float MAX_ARC_DIFF = 60f;
    public static final float[] BASE_RANGE = new float[] {500f, 600f, 700f, 800f};

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ReactiveFortressShield)
                                 .params(getSystemName(),
                                         Utils.asPercent(strength),
                                         Utils.asFloatOneDecimal(strength*5000f),
                                         Utils.asFloatTwoDecimals(strength*10000f));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.ReactiveFortressShieldPost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asFloatOneDecimal(selectedModule.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE[Utils.hullSizeToInt(selectedModule.getHullSize())])),
                Utils.asFloatOneDecimal(2f * MAX_ARC_DIFF));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(ReactiveFortressShieldScript.class)) {
            float strength = getStrength(ship);
            ship.addListener(new ReactiveFortressShieldScript(
                    ship,
                    strength,
                    strength*5000f,
                    strength*10000f,
                    ship.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE[Utils.hullSizeToInt(ship.getHullSize())])));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "fortressshield";
    }

    static class ReactiveFortressShieldScript implements DamageTakenModifier {
        final ShipAPI ship;
        final float chance, damage, empDamage, range;
        ReactiveFortressShieldScript(ShipAPI ship, float chance, float damage, float empDamage, float range) {
            this.ship = ship;
            this.chance = chance;
            this.damage = damage;
            this.empDamage = empDamage;
            this.range = range;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f point, boolean shieldHit) {
            if (!shieldHit || target != ship) return null;
            if (ship.getSystem().getEffectLevel() <= 0f) return null;
            // Don't react to beams
            if (damage.isDps()) return null;

            float chance = this.chance * ship.getSystem().getEffectLevel();
            if (Misc.random.nextFloat() <= chance) {
                ShipAPI targetShip = findTarget(point);
                if (targetShip != null) {
                    Global.getCombatEngine()
                          .spawnEmpArc(ship, point, ship, targetShip,
                                       DamageType.ENERGY,
                                       this.damage, // damage
                                       empDamage, // emp
                                       100000f, // max range
                                       "tachyon_lance_emp_impact",
                                       50f, // thickness
                                       ship.getShield().getInnerColor(),
                                       Color.WHITE);
                }
            }

            return null;
        }

        private ShipAPI findTarget(Vector2f point) {
            float dir = Misc.getAngleInDegrees(ship.getLocation(), point);
            ShipAPI closestTarget = null;
            float closestDist = Float.MAX_VALUE;
            for (ShipAPI target : Global.getCombatEngine().getShips()) {
                if (!target.isAlive() || target.getHitpoints() <= 0f) continue;
                if (!CollisionUtils.canCollide(target, null, ship, false)) continue;
                float dist = MathUtils.dist(point, target.getLocation());
                if (dist > Math.min(closestDist, range + target.getCollisionRadius())) continue;
                if (Misc.getAngleDiff(dir, Misc.getAngleInDegrees(point, target.getLocation())) > MAX_ARC_DIFF) continue;
                closestDist = dist;
                closestTarget = target;
            }
            return closestTarget;
        }
    }
}
