package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ReduceSModCreditsCost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return Utils.makeGenericNegatableDescription(getCostReduction(), Strings.SMOD_CREDITS_REDUCTION, Strings.SMOD_CREDITS_REDUCTION_NEG, true);
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_CREDITS_COST_MULT.modifyMult(id, 1 - getCostReduction());
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_CREDITS_COST_MULT.unmodify(id);
    }

    public float getCostReduction() {
        return Math.min(1f, getStrength() * 0.1f);
    }
}
