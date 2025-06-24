package shipmastery.ui.buttons;

import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import shipmastery.util.Strings;

public class CancelButton extends ButtonWithIcon {
    public CancelButton() {
        super("graphics/icons/ui/sms_cancel_icon.png", false);
    }

    @Override
    public void onClick() {
        finish();
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.cancelChangesTitle;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.setParaFont(Fonts.ORBITRON_12);
        String key = Keyboard.getKeyName(Keyboard.KEY_ESCAPE).toLowerCase();
        tooltip.addPara(Strings.MasteryPanel.hotkey, 0f, Misc.getGrayColor(), darkHighlightColor, key);
        tooltip.setParaFontDefault();
        tooltip.addPara(Strings.MasteryPanel.cancelChangesTooltipText, 10f);
    }
}
