package shipmastery.ui.buttons;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ui.LevelUpDialog;
import shipmastery.util.Strings;

public class LevelUpButton extends ButtonWithIcon {

    private final FleetMemberAPI member;

    public LevelUpButton(FleetMemberAPI member, boolean isAtMax) {
        super(isAtMax ? "graphics/icons/ui/sms_upgrade_icon_green.png" : "graphics/icons/ui/sms_upgrade_icon.png", isAtMax);
        this.member = member;
    }

    @Override
    public void onClick() {
        new LevelUpDialog(member, this::finish).show();
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.levelUpMastery;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.MasteryPanel.levelUpTooltip, 10f);
    }
}
