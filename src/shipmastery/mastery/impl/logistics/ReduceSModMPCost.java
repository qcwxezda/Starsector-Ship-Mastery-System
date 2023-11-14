package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ReduceSModMPCost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription.initDefaultHighlight(Strings.SMOD_MP_REDUCTION).params(getCostReduction(), 1);
    }

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.modifyFlat(id, getCostReduction());
    }

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.unmodify(id);
    }

    public int getCostReduction() {
        return Math.max(1, (int) getStrength());
    }
}
