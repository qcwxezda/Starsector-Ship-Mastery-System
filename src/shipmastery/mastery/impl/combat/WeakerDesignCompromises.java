package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class WeakerDesignCompromises extends BaseMasteryEffect {

    public static float ENERGY_FLUX_GENERATION_PENALTY_REDUCTION = 0.75f;

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.WeakerDesignCompromises)
                .params(Global.getSettings().getHullModSpec("design_compromises").getDisplayName(),
                    Utils.asPercent(getStrengthForPlayer()),
                    Utils.asPercent(ENERGY_FLUX_GENERATION_PENALTY_REDUCTION))
                .colors(Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getTextColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        var dissipationMod = stats.getFluxDissipation().getMultStatMod("design_compromises");
        var capacityMod = stats.getFluxCapacity().getMultStatMod("design_compromises");
        var generationMod = stats.getEnergyWeaponFluxCostMod().getPercentBonus("design_compromises");
        if (dissipationMod != null) {
            float mult = dissipationMod.getValue();
            stats.getFluxDissipation().modifyMult(id, 1 + (1-mult)/mult * getStrengthForPlayer());
        }
        if (capacityMod != null) {
            float mult = capacityMod.getValue();
            stats.getFluxCapacity().modifyMult(id, 1 + (1-mult)/mult * getStrengthForPlayer());
        }
        if (generationMod != null) {
            stats.getEnergyWeaponFluxCostMod().modifyPercent(id, -generationMod.getValue() * (1f - ENERGY_FLUX_GENERATION_PENALTY_REDUCTION));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isBuiltInMod("design_compromises")) return null;
        return 2f;
    }
}
