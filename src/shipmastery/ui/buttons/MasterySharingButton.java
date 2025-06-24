package shipmastery.ui.buttons;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;

import java.awt.Color;

public class MasterySharingButton extends ButtonWithIcon {

    private final ShipHullSpecAPI spec;

    public MasterySharingButton(ShipHullSpecAPI spec) {
        super("graphics/icons/ui/sms_construct_icon.png", false);
        this.spec = spec;
    }

    @Override
    public void onClick() {}

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.masterySharing;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        String active = button.isChecked() ? Strings.MasteryPanel.active : Strings.MasteryPanel.inactive;
        Color hc = Settings.POSITIVE_HIGHLIGHT_COLOR;
        tooltip.addPara(Strings.MasteryPanel.masterySharingTooltip, 10f, hc, "" + 200, "" + 50);
        tooltip.addPara(Strings.MasteryPanel.buttonStatus, 10f, hc, active);
    }
}
