package shipmastery.ui.buttons;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class RemoveSModsButton extends ButtonWithIcon {

    public RemoveSModsButton(boolean useSP) {
        super(useSP ? "graphics/icons/ui/sms_remove_smod_icon_green.png" : "graphics/icons/ui/sms_remove_smod_icon.png", useSP);
    }

    @Override
    public void onClick() {}

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.removeSModsButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        float frac = useStoryColors ? HullmodUtils.CREDITS_COST_MULT_SP : 1f;
        tooltip.addPara(Strings.MasteryPanel.removeSModsTooltip, 10f,
                useStoryColors ? Misc.getStoryBrightColor() : Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(HullmodUtils.SMOD_REMOVAL_COST_MULT * frac));
    }
}
