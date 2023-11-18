package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class SModMPCost extends AdditiveMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return makeGenericDescription(Strings.Descriptions.SModMPCost, Strings.Descriptions.SModMPCostNeg, true, getIncrease());
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.modifyFlat(id, getIncrease());
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.unmodify(id);
    }
}
