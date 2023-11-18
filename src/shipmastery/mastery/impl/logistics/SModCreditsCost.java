package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

public class SModCreditsCost extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return makeGenericDescription(
                Strings.Descriptions.SModCreditsCost,
                Strings.Descriptions.SModCreditsCostNeg,
                true, true, getIncrease());
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        modifyDefault(TransientSettings.SMOD_CREDITS_COST_MULT, id);
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_CREDITS_COST_MULT.unmodify(id);
    }
}