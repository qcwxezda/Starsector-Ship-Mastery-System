package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BuiltInHSA extends HullmodPackage {

    public static final float REQ_NOT_MET_MULT = 1f/3f;

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.BuiltInHSA;
    }

    @Override
    protected String[] getDescriptionParams(ShipVariantAPI selectedVariant) {
        return new String[] {
                Utils.getHullmodName(HullMods.HIGH_SCATTER_AMP),
                Utils.asInt(getStrength(selectedVariant))
        };
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.BuiltInHSAPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asInt(getStrength(selectedVariant)*REQ_NOT_MET_MULT));
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.HIGH_SCATTER_AMP, true)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 1;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getBeamWeaponRangeBonus().modifyFlat(id, getStrength(stats));
    }

    @Override
    protected void applyIfRequirementNotMet(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getBeamWeaponRangeBonus().modifyFlat(id, getStrength(stats)*REQ_NOT_MET_MULT);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isBuiltInMod(HullMods.ADVANCEDOPTICS)) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float weightedTotal = wsc.se + 2f*wsc.me + 4f*wsc.le;
        if (weightedTotal <= 0) return null;
        return super.getSelectionWeight(spec) * Utils.getSelectionWeightScaledByValueIncreasing(weightedTotal, 0f, 3.5f, 10f);
    }
}
