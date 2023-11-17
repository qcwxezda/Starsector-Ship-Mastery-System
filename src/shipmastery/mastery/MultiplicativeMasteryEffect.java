package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Utils;
public abstract class MultiplicativeMasteryEffect extends BaseMasteryEffect {
    public final float getMult() {
        return Math.max(1f + getStrength(), 0f);
    }

    public final float getIncrease() {return Math.max(getStrength(), -1f);}

    public final void modifyDefault(MutableStat stat, String id) {
        modify(stat, id, getMult());
    }

    public final void modify(MutableStat stat, String id, float mult) {
        if (mult >= 1f) {
            stat.modifyPercent(id, 100f*(mult - 1f));
        }
        else {
            stat.modifyMult(id, mult);
        }
    }

    public final void modify(StatBonus stat, String id, float mult) {
        if (mult >= 1f) {
            stat.modifyPercent(id, 100f*(mult - 1f));
        }
        else {
            stat.modifyMult(id, mult);
        }
    }

    /** Uses the first param to determine whether effect is positive or negative */
    public static MasteryDescription makeGenericDescription(String positiveText, String negativeText, boolean showAsPercent, boolean invertColors, float... params) {
        float increase = params[0];
        Object[] newParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            newParams[i] = showAsPercent ? Utils.absValueAsPercent(params[i]) :   Math.abs(params[i]);
        }
        return MasteryDescription.init(increase >= 0f ? positiveText: negativeText)
                                 .params(newParams)
                                 .colors(invertColors != increase > 0f ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
    }
}
