package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EnhanceButtonPressed extends ActionListener {
    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    public EnhanceButtonPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        int mpCost = MasteryUtils.getEnhanceMPCost(spec);
        int spCost = MasteryUtils.getEnhanceSPCost(spec);
        int curSP = Global.getSector().getPlayerStats().getStoryPoints();

        boolean isLogisticsBoost = MasteryUtils.getEnhanceCount(spec) == MasteryUtils.bonusLogisticSlotEnhanceNumber - 1;
        String stringToUse = getConfirmString(spCost, isLogisticsBoost);

        String amountStr = Utils.asPercent(Settings.ENHANCE_AMOUNT);
        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                isLogisticsBoost ?
                String.format(stringToUse,
                        mpCost,
                        spCost,
                        curSP) :
                String.format(stringToUse,
                        amountStr,
                        mpCost,
                        spCost,
                        curSP),
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                500f,
                160f,
                new ConfirmEnhanceMasteries(masteryPanel, spec)
        );

        if (dialogData != null) {
            dialogData.textLabel.setAlignment(Alignment.TMID);
            if (isLogisticsBoost) {
                dialogData.textLabel.setHighlight(mpCost + " MP", spCost + " SP", curSP + " SP");
                dialogData.textLabel.setHighlightColors(Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
            }
            else {
                dialogData.textLabel.setHighlight(amountStr, mpCost + " MP", spCost + " SP", curSP + " SP");
                dialogData.textLabel.setHighlightColors(Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
            }
        }
    }

    private static String getConfirmString(int spCost, boolean isLogisticsBoost) {
        String stringToUse;
        if (spCost > 0) {
            if (isLogisticsBoost) {
                stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmText2;
            }
            else {
                stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmText;
            }
        }
        else {
            if (isLogisticsBoost) {
                stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmText2NoSP;
            }
            else {
                stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmTextNoSP;
            }
        }
        return stringToUse;
    }
}
