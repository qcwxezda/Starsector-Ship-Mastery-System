package shipmastery.mastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.config.Settings;
import shipmastery.util.Utils;

import java.awt.Color;

public abstract class MultiplicativeMasteryEffect extends BaseMasteryEffect {
    public final float getMult(PersonAPI commander) {
        return Math.max(1f + getStrength(commander), 0f);
    }

    public final float getIncrease(PersonAPI commander) {return Math.max(getStrength(commander), -1f);}

    public final float getMult(ShipAPI ship) {
        return Math.max(1f + getStrength(ship), 0f);
    }

    public final float getMultPlayer() {return getMult(Global.getSector().getPlayerPerson());}

    public final float getIncreasePlayer() {return getIncrease(Global.getSector().getPlayerPerson());}

    public final void modifyPlayer(MutableStat stat, String id) {
        modify(stat, id, getMultPlayer());
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
                colors[i] = invertColors != f > 0f ? Settings.POSITIVE_HIGHLIGHT_COLOR : Settings.NEGATIVE_HIGHLIGHT_COLOR;
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
        return makeGenericDescriptionStatic(positiveText, negativeText, getIncreasePlayer() >= 0f, showAsPercent, invertColors, params);
    }
}
