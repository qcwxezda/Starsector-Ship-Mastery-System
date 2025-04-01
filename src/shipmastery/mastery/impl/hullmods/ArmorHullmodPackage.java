package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ArmorHullmodPackage extends HullmodPackage {
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ArmorHullmodPackagePost, 0f);
    }

    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.ArmorHullmodPackage;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.HEAVYARMOR),
                Utils.getHullmodName(HullMods.ARMOREDWEAPONS),
                Utils.asPercent(getStrength(selectedModule))};
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
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return super.getSelectionWeight(spec) * Utils.getSelectionWeightScaledByValueDecreasing(Utils.getShieldToHullArmorRatio(spec), 0f, 1f, 2.5f);
    }
}
