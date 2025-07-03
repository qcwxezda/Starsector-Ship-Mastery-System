package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.ui.LevelUpDialog;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class LevelUpButton extends ButtonWithIcon {

    private final FleetMemberAPI member;

    public LevelUpButton(FleetMemberAPI member, boolean isAtMax) {
        super(isAtMax ? "graphics/icons/ui/sms_upgrade_icon_green.png" : "graphics/icons/ui/sms_upgrade_icon.png", isAtMax);
        this.member = member;
    }

    public boolean isEnhance() {
        var spec = member.getHullSpec();
        return ShipMastery.getPlayerMasteryLevel(spec) >= ShipMastery.getMaxMasteryLevel(spec);
    }

    @Override
    public void onClick() {
        new LevelUpDialog(member, this::finish).show();
    }

    @Override
    public String getTooltipTitle() {
        return isEnhance() ? String.format(
                Strings.MasteryPanel.enhanceMastery,
                MasteryUtils.getEnhanceCount(member.getHullSpec()),
                MasteryUtils.MAX_ENHANCES)
                : Strings.MasteryPanel.levelUpMastery;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        if (isEnhance()) {
            tooltip.addPara(
                    Strings.MasteryPanel.enhanceMasteryTooltip,
                    10f,
                    Settings.POSITIVE_HIGHLIGHT_COLOR,
                    Utils.asPercent(MasteryUtils.getModifiedMasteryEffectStrength(Global.getSector().getPlayerPerson(), member.getHullSpec(), 1f)));
        }
        else {
            tooltip.addPara(Strings.MasteryPanel.levelUpTooltip, 10f);
        }
    }
}
