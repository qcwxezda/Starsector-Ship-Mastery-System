package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class FlatArmorHullDamage extends BaseMasteryEffect {

    public static final float[] MAX_EFFECTIVE_ARMOR = {30f, 60f, 90f, 120f};
    public static final float BASE_HULL_FRAC_REQUIRED = 1f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.FlatArmorHullDamage)
                .params(Utils.asInt(MAX_EFFECTIVE_ARMOR[Utils.hullSizeToInt(selectedVariant.getHullSize())]))
                .colors(Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.FlatArmorHullDamagePost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercentNoDecimal(BASE_HULL_FRAC_REQUIRED / getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.addListener(new FlatArmorHullDamageScript(ship, BASE_HULL_FRAC_REQUIRED / getStrength(ship)));
    }

    private class FlatArmorHullDamageScript implements HullDamageAboutToBeTakenListener {
        private final ShipAPI ship;
        private final float hullFracRequired;
        private float damageTaken = 0f;

        private FlatArmorHullDamageScript(ShipAPI ship, float hullFracRequired) {
            this.ship = ship;
            this.hullFracRequired = hullFracRequired;
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (damageAmount <= 0f) return false;
            damageTaken += damageAmount;
            float damageFrac = Math.min(1f, damageTaken / (hullFracRequired*ship.getMaxHitpoints()));
            float effectiveArmorBonus = damageFrac * MAX_EFFECTIVE_ARMOR[Utils.hullSizeToInt(ship.getHullSize())];
            this.ship.getMutableStats().getEffectiveArmorBonus().modifyFlat(id, effectiveArmorBonus);
            return false;
        }
    }
}
