package shipmastery.mastery.impl.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class BuiltInHSA extends HullmodPackage {
    @Override
    protected String getDescriptionString() {
        return Strings.Descriptions.BuiltInHSA;
    }

    @Override
    protected String[] getDescriptionParams(ShipAPI selectedModule) {
        return new String[] {
                Utils.getHullmodName(HullMods.HIGH_SCATTER_AMP),
                Utils.asInt(getStrength(selectedModule))
        };
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
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isBuiltInMod(HullMods.ADVANCEDOPTICS)) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float weightedTotal = wsc.se + 2f*wsc.me + 4f*wsc.le;
        if (weightedTotal <= 0) return null;
        return super.getSelectionWeight(spec) * Utils.getSelectionWeightScaledByValueIncreasing(weightedTotal, 0f, 3.5f, 10f);
    }
}
