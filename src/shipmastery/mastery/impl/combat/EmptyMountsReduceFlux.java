package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EmptyMountsReduceFlux extends BaseMasteryEffect {

    public static final float HARD_CAP = 0.2f;

    public float getReductionMultiplier(WeaponAPI.WeaponSize size) {
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
        return base;
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EmptyMountsReduceFlux)
                                 .params(Utils.asPercent(strength * getReductionMultiplier(WeaponAPI.WeaponSize.SMALL)),
                                         Utils.asPercent(strength * getReductionMultiplier(WeaponAPI.WeaponSize.MEDIUM)),
                                         Utils.asPercent(strength * getReductionMultiplier(WeaponAPI.WeaponSize.LARGE)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.EmptyMountsReduceFluxPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(HARD_CAP));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;

        float boost = 0f;
        for (WeaponSlotAPI slot : variant.getHullSpec().getAllWeaponSlotsCopy()) {
            if (slot.isBuiltIn() || !slot.isWeaponSlot() || slot.isDecorative()) continue;
            if (variant.getWeaponId(slot.getId()) == null) {
                boost += getStrength(stats) * getReductionMultiplier(variant.getSlot(slot.getId()).getSlotSize());
            }
        }
        boost = Math.min(boost, HARD_CAP);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - boost);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - boost);
        stats.getMissileWeaponFluxCostMod().modifyMult(id, 1f - boost);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getAllWeaponSlotsCopy().size() <= 1) return null;
        return 1f;
    }
}
