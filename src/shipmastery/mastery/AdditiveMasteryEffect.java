package shipmastery.mastery;

import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;

public abstract class AdditiveMasteryEffect extends BaseMasteryEffect {
    public final int getIncrease() {
        float f = getStrength();
        if (f > 0 && f < 1) return 1;
        if (f < 0 && f > -1) return -1;
        return (int) f;
    }

    /** Uses the first param to determine if the effect is positive or negative */
    public static MasteryDescription makeGenericDescriptionStatic(String positiveText, @Nullable String negativeText, boolean invertColors, int... params) {
        float increase = params[0];
        Object[] newParams = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            newParams[i] = negativeText == null ? params[i] : Math.abs(params[i]);
        }
        return MasteryDescription.init(increase >= 0f || negativeText == null ? positiveText: negativeText)
                                 .params(newParams)
                                 .colors(invertColors != increase >= 0f ? Misc.getHighlightColor() : Misc.getNegativeHighlightColor());
    }
}
