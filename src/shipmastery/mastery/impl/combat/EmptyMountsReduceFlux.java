package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EmptyMountsReduceFlux extends BaseMasteryEffect {

    public float getReduction(WeaponAPI.WeaponSize size) {
        int hullSize = Utils.hullSizeToInt(getHullSpec().getHullSize());
        float base = 1f;
        switch (size) {
            case MEDIUM:
                base = 3f; break;
            case LARGE:
                base = 6f; break;
            default: break;
        }
        base *= 4f - hullSize;
        return 0.01f * base;
    }

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.EmptyMountsReduceFlux)
                .params(Utils.asPercent(getReduction(WeaponAPI.WeaponSize.SMALL)),
                                         Utils.asPercent(getReduction(WeaponAPI.WeaponSize.MEDIUM)),
                                         Utils.asPercent(getReduction(WeaponAPI.WeaponSize.LARGE)))
                .colors(Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.EmptyMountsReduceFluxPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;

        float boost = 0f;
        for (WeaponSlotAPI slot : variant.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot.isBuiltIn() || !slot.isWeaponSlot() || slot.isDecorative()) continue;
            if (variant.getWeaponId(slot.getId()) == null) {
                boost += getReduction(variant.getSlot(slot.getId()).getSlotSize());
            }
        }
        boost = Math.min(boost, getStrength(stats));
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - boost);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - boost);
        stats.getMissileWeaponFluxCostMod().modifyMult(id, 1f - boost);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getAllWeaponSlotsCopy().size() <= 3) return null;
        return 1f;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f; // NPCs seem to prioritize fitting weapons
    }
}
