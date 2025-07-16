package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
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

public class NovaBurstDamage extends ShipSystemEffect {

    public static final float RANGE = 1000f;
    public static final float ARC_DEGREES = 60f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.NovaBurstDamage).params(getSystemName());
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        ship.addListener(new NovaBurstDamageScript(ship, getStrength(ship)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.NovaBurstDamagePost,
                0f,
                new Color[]{Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getTextColor(), Misc.getTextColor()},
                Utils.asInt(getStrengthForPlayer()),
                DamageType.ENERGY.getDisplayName(),
                Utils.asInt(ARC_DEGREES),
                Utils.asInt(RANGE));
    }

    @Override
    public String getSystemSpecId() {
        return "nova_burst";
    }

    record NovaBurstDamageScript(ShipAPI ship, float damage) implements ProjectileCreatedListener {
        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!"nova_burst_bomb".equals(proj.getProjectileSpecId())) return;
            CombatDeferredActionPlugin.performLater(() -> {
                Vector2f location = proj.getLocation();
                float angle = proj.getFacing();
                Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(location, 2f * RANGE, 2f * RANGE);
                while (itr.hasNext()) {
                    Object o = itr.next();
                    if (!(o instanceof CombatEntityAPI entity)) continue;
                    if (MathUtils.dist(entity.getLocation(), location) > RANGE + entity.getCollisionRadius()) continue;
                    if (Math.abs(MathUtils.angleDiff(angle, Misc.getAngleInDegrees(location, entity.getLocation()))) > ARC_DEGREES / 2f)
                        continue;
                    if (!CollisionUtils.canCollide(entity, null, ship, false)) continue;
                    Global.getCombatEngine().spawnEmpArc(
                            ship,
                            location,
                            null,
                            entity,
                            DamageType.ENERGY,
                            damage,
                            damage,
                            1000000f,
                            "tachyon_lance_emp_impact",
                            80f,
                            new Color(100, 165, 255, 255),
                            Color.WHITE).setCoreWidthOverride(40f);
                }
            }, 0.25f);
        }
    }
}
