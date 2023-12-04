package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.deferred.Action;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.Iterator;

public class NovaBurstDamage extends BaseMasteryEffect {

    public static final float RANGE = 1000f;
    public static final float ARC_DEGREES = 60f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.NovaBurstDamage).params(selectedModule.getSystem().getDisplayName());
    }
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null | !"nova_burst".equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(NovaBurstDamageScript.class)) {
            ship.addListener(new NovaBurstDamageScript(ship, getStrength(ship)));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.NovaBurstDamagePost,
                0f,
                new Color[] {Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getTextColor(), Misc.getTextColor()},
                "" + (int) getStrengthForPlayer(),
                DamageType.ENERGY.getDisplayName(),
                "" + (int) ARC_DEGREES,
                "" + (int) RANGE);
    }

    static class NovaBurstDamageScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float damage;
        WeaponAPI bombLauncher;

        NovaBurstDamageScript(ShipAPI ship, float damage) {
            this.ship = ship;
            this.damage = damage;

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if ("pusherplate_nova".equals(weapon.getId())) {
                    bombLauncher = weapon;
                    return;
                }
            }
        }

        @Override
        public void onActivate() {
            CombatDeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    Vector2f location = bombLauncher.getLocation();
                    float angle = bombLauncher.getCurrAngle();
                    Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(location, 2f*RANGE, 2f*RANGE);
                    while (itr.hasNext()) {
                        Object o = itr.next();
                        if (!(o instanceof CombatEntityAPI)) continue;
                        CombatEntityAPI entity = (CombatEntityAPI) o;
                        if (MathUtils.dist(entity.getLocation(), location) > RANGE + entity.getCollisionRadius()) continue;
                        if (Math.abs(MathUtils.angleDiff(angle, Misc.getAngleInDegrees(location, entity.getLocation()))) > ARC_DEGREES / 2f) continue;
                        if (!CollisionUtils.canCollide(entity, null, ship, false)) continue;
                        Global.getCombatEngine().spawnEmpArc(
                                ship,
                                location,
                                ship,
                                entity,
                                DamageType.ENERGY,
                                damage,
                                damage,
                                1000000f,
                                "tachyon_lance_emp_impact",
                                80f,
                                new Color(100,165,255,255),
                                Color.WHITE);
                    }
                }
            }, 0.5f);
        }
    }
}
