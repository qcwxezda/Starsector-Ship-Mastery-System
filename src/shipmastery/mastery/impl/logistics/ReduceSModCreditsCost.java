package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ReduceSModCreditsCost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.initDefaultHighlight(Strings.SMOD_CREDITS_REDUCTION)
                                 .params((int) (100f * getCostReduction()) + "%");

    }

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_CREDITS_COST_MULT.modifyMult(id, 1 - getCostReduction());
    }

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_CREDITS_COST_MULT.unmodify(id);
    }

    public float getCostReduction() {
        return Math.min(1f, getStrength() * 0.1f);
    }
}
