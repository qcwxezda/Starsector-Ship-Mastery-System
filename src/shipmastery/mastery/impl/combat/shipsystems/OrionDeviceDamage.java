package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.config.Settings;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Iterator;

public class OrionDeviceDamage extends ShipSystemEffect {

    public static final float RANGE = 750f;
    public static final float ARC_DEGREES = 75f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.OrionDeviceDamage).params(getSystemName());
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        ship.addListener(new OrionDeviceDamageScript(ship, getStrength(ship)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.OrionDeviceDamagePost,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getTextColor(), Misc.getTextColor()},
                Utils.asInt(getStrengthForPlayer()),
                DamageType.HIGH_EXPLOSIVE.getDisplayName(),
                Utils.asInt(ARC_DEGREES),
                Utils.asInt(RANGE));
    }

    @Override
    public String getSystemSpecId() {
        return "orion_device";
    }

    static class OrionDeviceDamageScript implements ProjectileCreatedListener {
        final ShipAPI ship;
        final float damage;
        WeaponAPI bombLauncher;

        OrionDeviceDamageScript(ShipAPI ship, float damage) {
            this.ship = ship;
            this.damage = damage;

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getId().startsWith("pusherplate_lt")) {
                    bombLauncher = weapon;
                    return;
                }
            }
        }

        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!"orion_device_bomb".equals(proj.getProjectileSpecId())) return;
            CombatDeferredActionPlugin.performLater(() -> {
                Vector2f location = bombLauncher.getLocation();
                float angle = bombLauncher.getCurrAngle();
                Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(location, 2f*RANGE, 2f*RANGE);
                while (itr.hasNext()) {
                    Object o = itr.next();
                    if (!(o instanceof CombatEntityAPI entity)) continue;
                    if (MathUtils.dist(entity.getLocation(), location) > RANGE + entity.getCollisionRadius()) continue;
                    if (Math.abs(MathUtils.angleDiff(angle, Misc.getAngleInDegrees(location, entity.getLocation()))) > ARC_DEGREES / 2f) continue;
                    if (!CollisionUtils.canCollide(entity, null, ship, false)) continue;
                    Pair<Vector2f, Boolean> collisionPoint = CollisionUtils.rayCollisionCheckEntity(location, entity.getLocation(), entity);
                    if (collisionPoint.one == null) continue;
                    Global.getCombatEngine().applyDamage(
                            proj,
                            entity,
                            collisionPoint.one,
                            ship.getMutableStats().getMissileWeaponDamageMult().getModifiedValue() * damage,
                            DamageType.HIGH_EXPLOSIVE,
                            0f,
                            false,
                            false,
                            ship,
                            true);
                }
            }, 0.25f);
        }
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return !fm.isFlagship() ? 0f : super.getNPCWeight(fm);
    }
}
