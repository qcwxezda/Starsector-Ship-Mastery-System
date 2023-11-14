package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ReduceRestorationCost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.initDefaultHighlight(Strings.REDUCE_RESTORATION_COST).params((int) (getMult()*100f) + "%");
    }

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {
        float mult = Global.getSettings().getFloat("baseRestoreCostMult");
        Global.getSettings().setFloat("baseRestoreCostMult", (1f-getMult())*mult);
    }

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {
        float mult = Global.getSettings().getFloat("baseRestoreCostMult");
        Global.getSettings().setFloat("baseRestoreCostMult", 1f/(1f-getMult())*mult);
    }

    float getMult() {
        return Math.min(0.99f, 0.1f * getStrength());
    }
}
