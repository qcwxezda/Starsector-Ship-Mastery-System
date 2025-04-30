package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class BeamPartialHardFlux extends BaseMasteryEffect {

    public static final float CLOSE_RANGE_FRAC = 0.8f;
    public static final float LONG_RANGE_FRAC = 0f;
    public static final float MIN_RANGE_FRAC = 0f;
    public static final float BASE_MAX_RANGE_FRAC = 0.7f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.BeamPartialHardFlux);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.BeamPartialHardFluxPost,
                0f,
                new Color[]{Misc.getTextColor(), Misc.getTextColor(), Misc.getTextColor(), Settings.POSITIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(CLOSE_RANGE_FRAC),
                Utils.asPercent(MIN_RANGE_FRAC),
                Utils.asPercent(LONG_RANGE_FRAC),
                Utils.asPercent(BASE_MAX_RANGE_FRAC * getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new BeamPartialHardFluxListener(ship, BASE_MAX_RANGE_FRAC * getStrength(ship)));
    }

    private class BeamPartialHardFluxListener implements DamageDealtModifier {
        private final ShipAPI ship;
        private final float maxRangeFrac;
        private boolean noRecurse = false;

        private BeamPartialHardFluxListener(ShipAPI ship, float maxRangeFrac) {
            this.ship = ship;
            this.maxRangeFrac = maxRangeFrac;
        }

        // new DamageAPI per distinct beam, not per hit frame.
        // setForceHardFlux only supports one effect/beam using it, multiple effects using this setting would conflict
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (noRecurse) {
                return null;
            }
            if (!shieldHit || !(param instanceof BeamAPI beam)) return null;
            if (damage.isForceHardFlux()) return null;
            WeaponAPI weapon = beam.getWeapon();
            if (weapon == null) return null;
            float rangeFrac = MathUtils.dist(weapon.getLocation(), point) / weapon.getRange();
            float hardFluxFrac = MathUtils.lerp(CLOSE_RANGE_FRAC, LONG_RANGE_FRAC, (rangeFrac - MIN_RANGE_FRAC) / (maxRangeFrac - MIN_RANGE_FRAC));
            hardFluxFrac = Math.max(0f, hardFluxFrac);
            hardFluxFrac = Math.min(CLOSE_RANGE_FRAC, hardFluxFrac);

            if (hardFluxFrac <= 0f) return null;

            float origDamage = damage.getDamage();
            float origFluxComp = damage.getFluxComponent();

            damage.getModifier().modifyMult(id, 1f - hardFluxFrac);
            noRecurse = true;
            // damage modifier doesn't get propagated, so pass getDamage instead of getBaseDamage
            Global.getCombatEngine().applyDamage(
                    beam,
                    target,
                    point,
                    origDamage*damage.getDpsDuration()*hardFluxFrac,
                    damage.getType(),
                    origFluxComp*damage.getDpsDuration()*hardFluxFrac,
                    false,
                    false,
                    ship,
                    false
            );
            noRecurse = false;
            return id;
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        var wsc = Utils.countWeaponSlots(spec);
        float energyWeight = wsc.computeWeaponWeight(WeaponAPI.WeaponType.ENERGY, 0.2f, 0.3f);
        if (energyWeight <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(energyWeight, 0f, 0.4f, 1f);
    }
}
