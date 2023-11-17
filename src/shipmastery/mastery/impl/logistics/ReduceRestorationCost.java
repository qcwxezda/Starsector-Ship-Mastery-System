package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class ReduceRestorationCost extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return makeGenericDescription(
                Strings.REDUCE_RESTORATION_COST,
                Strings.REDUCE_RESTORATION_COST_NEG,
                true, true, getIncrease());
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        modifyDefault(TransientSettings.SHIP_RESTORE_COST_MULT, id);
        Global.getSettings().setFloat("baseRestoreCostMult", TransientSettings.SHIP_RESTORE_COST_MULT.getModifiedValue());
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SHIP_RESTORE_COST_MULT.unmodify(id);
        Global.getSettings().setFloat("baseRestoreCostMult", TransientSettings.SHIP_RESTORE_COST_MULT.getModifiedValue());
    }
}
