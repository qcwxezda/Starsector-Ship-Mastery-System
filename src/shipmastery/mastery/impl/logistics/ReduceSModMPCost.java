package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ReduceSModMPCost extends AdditiveMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return makeGenericDescription(Strings.SMOD_MP_REDUCTION, Strings.SMOD_MP_REDUCTION_NEG, true, getIncrease());
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
