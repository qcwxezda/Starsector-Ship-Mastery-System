package shipmastery.mastery;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.config.Settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

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
     *  match params in length. Each element may be null, in which case no highlight is applied for the corresponding param. */
    public Color[] colors;
    private boolean checkedColors = false;

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
        checkedColors = true;
    }

    public static MasteryDescription init(String formatText) {
        MasteryDescription description = new MasteryDescription();
        description.text = formatText;
        return description;
    }

    public static MasteryDescription initDefaultHighlight(String formatText) {
        return init(formatText).colors(Settings.POSITIVE_HIGHLIGHT_COLOR);
    }

    public MasteryDescription params(Object... params) {
        this.params = params;
        if (!checkedColors) {
            checkColors();
        }
        return this;
    }

    public MasteryDescription colors(Color... colors) {
        this.colors = colors;
        if (params != null && !checkedColors) {
            checkColors();
        }
        return this;
    }

    public void addLabel(TooltipMakerAPI tooltip) {
        addLabelWithPrefix(tooltip, null, null);
    }

    public void addLabelWithPrefix(TooltipMakerAPI tooltip, @Nullable String prefix, @Nullable Color prefixColor) {
        Color textColor = Misc.getTextColor();
        if (prefix != null) {
            tooltip.setParaFont(Fonts.ORBITRON_20AA);
            tooltip.addPara(prefix, prefixColor == null ? textColor : prefixColor, 0f);
        }
        tooltip.setParaFont(Fonts.INSIGNIA_LARGE);
        List<Color> newColors = new ArrayList<>();
        List<String> newParams = new ArrayList<>();
        if (colors != null) {
            for (Color color : colors) {
                newColors.add(color == null ? textColor : color);
            }
            if (params != null) {
                for (Object param : params) {
                    newParams.add(param.toString());
                }
            }
            for (int i = newColors.size(); i < newParams.size(); i++) {
                newColors.add(colors.length == 0 ? textColor : colors[0]);
            }
        }
        tooltip.addPara(text, 0f, newColors.toArray(new Color[0]), newParams.toArray(new String[0])).setAlignment(Alignment.LMID);
    }


    @Override
    public String toString(){
        return String.format(text, params);
    }
}
