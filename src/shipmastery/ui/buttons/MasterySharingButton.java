package shipmastery.ui.buttons;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.campaign.MasterySharingHandler;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class MasterySharingButton extends ButtonWithIcon {

    private final ShipHullSpecAPI spec;

    public MasterySharingButton(ShipHullSpecAPI spec) {
        super("graphics/icons/ui/sms_construct_icon.png", false);
        this.spec = spec;
    }

    @Override
    public void onClick() {
        boolean isActive = !MasterySharingHandler.isMasterySharingActive(spec);
        MasterySharingHandler.modifyMasterySharingStatus(spec, isActive);
        thisButton.setChecked(isActive);
        finish();
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.masterySharing;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        String active = thisButton.isChecked() ? Strings.MasteryPanel.active : Strings.MasteryPanel.inactive;
        Color hc = Settings.POSITIVE_HIGHLIGHT_COLOR;
        tooltip.addPara(
                Strings.MasteryPanel.masterySharingTooltip,
                10f,
                new Color[] {Settings.NEGATIVE_HIGHLIGHT_COLOR, hc, hc},
                Utils.asPercent(1f - MasterySharingHandler.SHARED_MASTERY_MP_MULT),
                Utils.asInt(MasterySharingHandler.SHARED_MASTERY_MP_REQ),
                Utils.asInt(MasterySharingHandler.SHARED_MASTERY_MP_GAIN));
        tooltip.addPara(Strings.MasteryPanel.buttonStatus, 10f, hc, active);
    }
}
