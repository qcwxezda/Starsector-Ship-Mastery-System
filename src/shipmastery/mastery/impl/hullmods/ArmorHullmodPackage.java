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

public class ArmorHullmodPackage extends HullmodPackage {

    public static float REQ_NOT_MET_MULT = 10f/12f;

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(
                Strings.Descriptions.ArmorHullmodPackagePost,
                0f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercentNoDecimal(getStrength(selectedVariant)*REQ_NOT_MET_MULT));
    }

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.ArmorHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipVariantAPI selectedVariant) {
        return new String[] {
                Utils.getHullmodName(HullMods.HEAVYARMOR),
                Utils.getHullmodName(HullMods.ARMOREDWEAPONS),
                Utils.asPercentNoDecimal(getStrength(selectedVariant))};
    }

    @Override
    protected HullmodData[] getHullmodList() {
        return new HullmodData[] {
                new HullmodData(HullMods.HEAVYARMOR, false),
                new HullmodData(HullMods.ARMOREDWEAPONS, false)
        };
    }

    @Override
    protected int getRequiredCount() {
        return 2;
    }

    @Override
    protected void apply(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getArmorBonus().modifyMult(id, 1f + getStrength(stats));
    }

    @Override
    protected void applyIfRequirementNotMet(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getArmorBonus().modifyPercent(id, 100f*getStrength(stats)*REQ_NOT_MET_MULT);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return super.getSelectionWeight(spec) * Utils.getSelectionWeightScaledByValueDecreasing(Utils.getShieldToHullArmorRatio(spec), 0f, 1f, 2.5f);
    }
}
