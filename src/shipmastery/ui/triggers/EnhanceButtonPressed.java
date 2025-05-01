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

        int count = MasteryUtils.getEnhanceCount(spec);
        boolean isLogisticsBoost = count == MasteryUtils.bonusLogisticSlotEnhanceNumber - 1;
        float dr = Settings.ENHANCE_DR_AMOUNT[count];
        boolean hasDR = dr > 0f;
        String stringToUse = getConfirmString(spCost, hasDR, isLogisticsBoost);

        String amountStr = Utils.asPercent(Settings.ENHANCE_MASTERY_AMOUNT[count]);
        String drString = Utils.asPercent(dr);
        String bonusXPString = Utils.asPercent(Settings.ENHANCE_BONUS_XP[count]);
        String format;

        if (isLogisticsBoost) {
            format = String.format(stringToUse, mpCost, spCost, bonusXPString, curSP);
        } else if (!hasDR) {
            format = String.format(stringToUse, amountStr, mpCost, spCost, bonusXPString, curSP);
        } else {
            if (spCost > 0) {
                format = String.format(stringToUse, amountStr, drString, mpCost, spCost, bonusXPString, curSP);
            } else {
                format = String.format(stringToUse, amountStr, mpCost);
            }
        }

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                format,
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                550f,
                180f,
                new ConfirmEnhanceMasteries(masteryPanel, spec)
        );

        if (dialogData != null) {
            dialogData.textLabel.setAlignment(Alignment.TL);
            if (isLogisticsBoost) {
                dialogData.textLabel.setHighlight(mpCost + " MP", spCost + " SP", bonusXPString, curSP + " SP");
                dialogData.textLabel.setHighlightColors(Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
            }
            else if (!hasDR) {
                dialogData.textLabel.setHighlight(amountStr, mpCost + " MP", spCost + " SP", bonusXPString, curSP + " SP");
                dialogData.textLabel.setHighlightColors(Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
            }
            else {
                dialogData.textLabel.setHighlight(amountStr, drString, mpCost + " MP", spCost + " SP", bonusXPString, curSP + " SP");
                dialogData.textLabel.setHighlightColors(Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
            }
        }
    }

    private static String getConfirmString(int spCost, boolean hasDR, boolean isLogisticsBoost) {
        String stringToUse;
        if (spCost > 0) {
            if (isLogisticsBoost) {
                stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmText2;
            }
            else {
                if (!hasDR) {
                    stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmText;
                } else {
                    stringToUse = Strings.MasteryPanel.enhanceMasteryConfirmText3;
                }
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
