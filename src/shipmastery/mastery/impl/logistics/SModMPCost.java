package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class SModMPCost extends AdditiveMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(Strings.Descriptions.SModMPCost, Strings.Descriptions.SModMPCostNeg, true, getIncrease());
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.modifyFlat(id, getIncrease());
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.unmodify(id);
    }
}
