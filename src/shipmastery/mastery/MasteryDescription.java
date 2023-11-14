package shipmastery.mastery;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

/** Description for a mastery effect. The actual displayed text will be {@code String.format(text, params)}.
 *  If {@code colors} is specified, {@code params} will be highlighted in the text.
 *
 * @see String#format
 *  */
public class MasteryDescription {
    /** Will have {@link String#format(String, Object...)} called on it */
    public String text;
    /** Variables to replace format specifiers in {@code rawText} with */
    public Object[] params;
    /** Colors to highlight the params. Must either be a single element array (highlights all params with one color) or
     *  match params in length. */
    public Color[] colors;

    public MasteryDescription() {}

    public MasteryDescription(String text, Object[] params, Color... colors) {
        this.text = text;
        this.params = params;
        this.colors = colors;
        checkColors();
    }

    void checkColors() {
        boolean validColors = colors == null || colors.length == 1 || colors.length == params.length;
        if (!validColors) {
            throw new RuntimeException("Invalid colors array in MasteryDescription");
        }
    }

    public static MasteryDescription init(String formatText) {
        MasteryDescription description = new MasteryDescription();
        description.text = formatText;
        return description;
    }

    public static MasteryDescription initDefaultHighlight(String formatText) {
        return init(formatText).colors(Misc.getHighlightColor());
    }

    public MasteryDescription params(Object... params) {
        this.params = params;
        return this;
    }

    public MasteryDescription colors(Color... colors) {
        this.colors = colors;
        checkColors();
        return this;
    }

    public void addLabel(TooltipMakerAPI tooltip) {
        Color textColor = Misc.getTextColor();
        LabelAPI label = tooltip.addPara(toString(), textColor, 0f);
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
        label.setAlignment(Alignment.LMID);
    }

    @Override
    public String toString(){
        return String.format(text, params);
    }
}
