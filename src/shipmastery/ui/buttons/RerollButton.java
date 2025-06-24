package shipmastery.ui.buttons;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.Strings;

public class RerollButton extends ButtonWithIcon {
    public RerollButton() {
        super("graphics/icons/ui/sms_reroll_icon_green.png", true);
    }

    @Override
    public void onClick() {}

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.rerollMasteries;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.MasteryPanel.rerollTooltip, 10f);
    }
}
