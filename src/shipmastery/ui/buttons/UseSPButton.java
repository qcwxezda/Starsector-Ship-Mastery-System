package shipmastery.ui.buttons;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class UseSPButton extends ButtonWithIcon {

    public UseSPButton() {
        super("graphics/icons/ui/sms_lightbulb_icon.png", false);
    }

    @Override
    public void onClick() {
        finish();
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.useSPButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.MasteryPanel.useSPTooltip, 10f, Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(1f - HullmodUtils.CREDITS_COST_MULT_SP),
                Misc.getDGSCredits(HullmodUtils.CREDITS_COST_BXP_CAP));
        tooltip.addPara(Strings.MasteryPanel.buttonStatus, 10f, Settings.POSITIVE_HIGHLIGHT_COLOR,
                thisButton.isChecked() ? Strings.MasteryPanel.active : Strings.MasteryPanel.inactive);
    }
}
