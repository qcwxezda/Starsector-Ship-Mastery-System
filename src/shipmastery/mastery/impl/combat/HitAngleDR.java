package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class HitAngleDR extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(Strings.Descriptions.HitAngleDR, Strings.Descriptions.HitAngleDRNeg, true, true, getIncreasePlayer());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.HitAngleDRPost, 0f);
        tooltip.addPara(Strings.Descriptions.HitAngleDRPost2, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(HitAngleDRScript.class)) {
            ship.addListener(new HitAngleDRScript(ship, getMult(ship), id));
        }
    }

    static class HitAngleDRScript implements DamageTakenModifier {

        final ShipAPI ship;
        final float mult;
        final String id;
        BoundsAPI.SegmentAPI lastSegmentHit;

        HitAngleDRScript(ShipAPI ship, float mult, String id) {
            this.ship = ship;
            this.mult = mult;
            this.id = id;
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                                        Vector2f pt, boolean shieldHit) {
            if (shieldHit) return null;
            if (param instanceof MissileAPI) return null;
            if (!(param instanceof BeamAPI || param instanceof DamagingProjectileAPI)) return null;

            BoundsAPI.SegmentAPI segment = CollisionUtils.getSegmentForHitPoint(ship, pt, lastSegmentHit);
            if (segment == null) return null;
            lastSegmentHit = segment;

            Vector2f a = new Vector2f(segment.getP1().x - segment.getP2().x, segment.getP1().y - segment.getP2().y);
            Vector2f b = param instanceof DamagingProjectileAPI ? Misc.getUnitVectorAtDegreeAngle(((DamagingProjectileAPI) param).getFacing()) : Misc.getUnitVector(((BeamAPI) param).getFrom(), ((BeamAPI) param).getTo());
            MathUtils.safeNormalize(a);
            MathUtils.safeNormalize(b);

            float effectLevel = Math.abs(Vector2f.dot(a, b));

            float newMult = (mult - 1f) * effectLevel + 1f;
            if (newMult > 1f) {
                damage.getModifier().modifyPercent(id, 100f * (newMult - 1f));
            }
            else {
                damage.getModifier().modifyMult(id, newMult);
            }
            return id;
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValueDecreasing(Utils.getShieldToHullArmorRatio(spec), 0f, 1f, 2.5f);
    }
}
