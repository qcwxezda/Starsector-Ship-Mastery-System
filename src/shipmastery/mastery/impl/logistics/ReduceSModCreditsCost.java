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
        return Utils.makeGenericNegatableDescription(1f - getMult(), Strings.SMOD_CREDITS_REDUCTION, Strings.SMOD_CREDITS_REDUCTION_NEG, true);
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        float mult = getMult();
        if (mult >= 1f) {
            TransientSettings.SMOD_CREDITS_COST_MULT.modifyPercent(id, 100f*mult);
        }
        else {
            TransientSettings.SMOD_CREDITS_COST_MULT.modifyMult(id, mult);
        }
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_CREDITS_COST_MULT.unmodify(id);
    }

    public float getMult() {
        return Math.min(0f, 1f - 0.1f * getStrength());
    }
}
