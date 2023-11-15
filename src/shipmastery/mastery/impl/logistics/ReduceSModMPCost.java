package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ReduceSModMPCost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        int r = getCostReduction();
        return MasteryDescription.init(r > 0 ? Strings.SMOD_MP_REDUCTION : Strings.SMOD_MP_REDUCTION_NEG)
                                 .params(Math.abs(getCostReduction()), 1).colors(r > 0 ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.modifyFlat(id, getCostReduction());
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.unmodify(id);
    }

    public int getCostReduction() {
        float f = 0.4f * getStrength();
        if (f > 0 && f < 1) f = 1;
        else if (f < 0 && f > -1) f = -1;
        return (int) f;
    }
}
