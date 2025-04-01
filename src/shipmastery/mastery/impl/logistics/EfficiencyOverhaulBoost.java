package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EfficiencyOverhaulBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EfficiencyOverhaulBoost).params(
                Global.getSettings().getHullModSpec(HullMods.EFFICIENCY_OVERHAUL).getDisplayName(),
                Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null) return;
        if (stats.getVariant().hasHullMod(HullMods.EFFICIENCY_OVERHAUL)) {
            float strength = getStrength(stats);
            stats.getSuppliesPerMonth().modifyMult(id, 1f - strength);
            stats.getFuelUseMod().modifyMult(id, 1f - strength);
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (!spec.isCivilianNonCarrier()) return 0f;
        float f = Utils.getSelectionWeightScaledByValueIncreasing(spec.getSuppliesPerMonth() + 4f*spec.getFuelPerLY(), 5f, 20f, 60f);
        if (spec.isBuiltInMod("high_maintenance")) f *= 2;
        return f;
    }
}
