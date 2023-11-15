package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ReduceRestorationCost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return Utils.makeGenericNegatableDescription(getMult(), Strings.REDUCE_RESTORATION_COST, Strings.REDUCE_RESTORATION_COST_NEG, true);
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        float mult = Global.getSettings().getFloat("baseRestoreCostMult");
        Global.getSettings().setFloat("baseRestoreCostMult", (1f - getMult()) * mult);
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        float mult = Global.getSettings().getFloat("baseRestoreCostMult");
        Global.getSettings().setFloat("baseRestoreCostMult", 1f / (1f - getMult()) * mult);
    }

    public float getMult() {
        return Math.min(0.99f, 0.1f * getStrength());
    }
}
