package shipmastery.ui.buttons;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.Strings;

public class IntegrateButton extends ButtonWithIcon {

    public IntegrateButton(boolean useSP) {
        super(useSP ? "graphics/icons/ui/sms_integrate_icon_green.png" : "graphics/icons/ui/sms_integrate_icon.png", useSP);
    }

    @Override
    public void onClick() {}

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.integrationButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.MasteryPanel.integrationTooltip, 10f);
    }
}
