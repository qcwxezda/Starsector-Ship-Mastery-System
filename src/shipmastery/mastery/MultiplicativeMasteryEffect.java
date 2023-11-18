package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.util.Utils;

import java.awt.*;

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

    public static MasteryDescription makeGenericDescriptionStatic(String positiveText, @Nullable String negativeText, boolean isPositive, boolean showAsPercent, boolean invertColors, Object... params) {
        Object[] newParams = new Object[params.length];
        Color[] colors = new Color[params.length];

        for (int i = 0; i < params.length; i++) {
            if (params[i] instanceof Float) {
                float f = (float) params[i];
                if (negativeText != null) {
                    newParams[i] = showAsPercent ? Utils.absValueAsPercent(f) : Math.abs(f);
                }
                else {
                    newParams[i] = showAsPercent ? Utils.asPercent(f) : f;
                }
                colors[i] = invertColors != f > 0f ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor();
            }
            else {
                newParams[i] = params[i];
                colors[i] = null;
            }
        }

        return MasteryDescription.init(isPositive || negativeText == null ? positiveText: negativeText)
                                 .params(newParams)
                                 .colors(colors);
    }

    public final MasteryDescription makeGenericDescription(String positiveText, @Nullable String negativeText, boolean showAsPercent, boolean invertColors, Object... params) {
        return makeGenericDescriptionStatic(positiveText, negativeText, getIncrease() >= 0f, showAsPercent, invertColors, params);
    }
}
