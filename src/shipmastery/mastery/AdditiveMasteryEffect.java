package shipmastery.mastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.util.Utils;

public abstract class AdditiveMasteryEffect extends BaseMasteryEffect {
    public final int getIncrease(PersonAPI commander) {
        float f = getStrength(commander);
        if (f > 0 && f < 1) return 1;
        if (f < 0 && f > -1) return -1;
        return (int) f;
    }

    public final int getIncrease(MutableShipStatsAPI stats) {
        return getIncrease(Utils.getCommanderForFleetMember(stats.getFleetMember()));
    }

    public final int getIncreasePlayer() {
        return getIncrease(Global.getSector().getPlayerPerson());
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
