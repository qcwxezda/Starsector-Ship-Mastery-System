package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.ui.EnhanceMasteryDisplay;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class EnhanceButtonPressed extends ActionListener {
    final MasteryPanel masteryPanel;
    final String defaultText;
    final ShipHullSpecAPI spec;

    public EnhanceButtonPressed(MasteryPanel masteryPanel, String defaultText, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.defaultText = defaultText;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        int mpCost = MasteryUtils.getEnhanceMPCost(spec);
        int spCost = MasteryUtils.getEnhanceSPCost(spec);
        int curSP = Global.getSector().getPlayerStats().getStoryPoints();

        String amountStr = Utils.asPercent(EnhanceMasteryDisplay.ENHANCE_AMOUNT);
        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                String.format(Strings.MasteryPanel.enhanceMasteryConfirmText,
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
            dialogData.textLabel.setHighlight(amountStr, mpCost + " MP", spCost + " SP", curSP + " SP");
            dialogData.textLabel.setHighlightColors(Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
        }
    }
}
