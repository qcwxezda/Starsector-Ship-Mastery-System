package shipmastery.mastery;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

/** Stores a description for a mastery effect. */
public class MasteryDescription {
    /** Will have {@link String#format(String, Object...)} called on it */
    final String text;
    /** Variables to replace format specifiers in {@code rawText} with */
    final Object[] params;
    /** Colors to highlight the params. Must either be a single element array (highlight all params with one color) or
     *  match params in length. */
    final Color[] colors;

    public MasteryDescription(String text, Object... params) {
        this(text, params, (Color[]) null);
    }

    public MasteryDescription(String text, Object[] params, Color... colors) {
        this.text = text;
        this.params = params;
        this.colors = colors;
    }

    public LabelAPI addLabel(TooltipMakerAPI tooltip) {
        Color textColor = Misc.getTextColor();
        LabelAPI label = tooltip.addPara("\n" + this + "\n", textColor, 0f);
        if (colors != null) {
            String[] strings = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                strings[i] = params[i].toString();
            }
            label.setHighlight(strings);
            if (colors.length == 1) {
                label.setHighlightColor(colors[0]);
            }
            else {
                label.setHighlightColors(colors);
            }
        }
        label.setAlignment(Alignment.MID);
        return label;
    }

    @Override
    public String toString(){
        return String.format(text, params);
    }
}
