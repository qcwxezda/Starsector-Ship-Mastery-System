package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Arrays;

public abstract class ButtonWithCost extends ButtonWithIcon {

    protected LabelAPI costLabel;
    protected LabelAPI spLabel;
    protected ButtonAPI confirmButton;

    public ButtonWithCost(String spriteName, boolean isStoryOption) {
        super(spriteName, isStoryOption);
        onFinish(() -> {
            float cost = getModifiedCost();
            Utils.getPlayerCredits().subtract(cost);
            if (isStoryOption) {
                Global.getSector().getPlayerStats().spendStoryPoints(
                        1,
                        true,
                        null,
                        true,
                        HullmodUtils.getBonusXPFraction(cost),
                        getUsedSPDescription());
                Global.getSoundPlayer().playUISound(getUsedSPSound(), 1f, 1f);
            } else {
                Global.getSoundPlayer().playUISound(getNormalSound(), 1f, 1f);
            }
        });
    }

    protected String getNormalSound() {
        return "sms_add_smod";
    }

    protected String getUsedSPSound() {
        return "ui_char_spent_story_point_industry";
    }

    protected final void updateLabels() {
        float baseCost = getBaseCost();
        float cost = getModifiedCost();
        boolean canAfford = cost <= Utils.getPlayerCredits().get();
        boolean hasEnoughSP = Global.getSector().getPlayerStats().getStoryPoints() >= 1;
        boolean shouldShow = shouldShowCostLabels();

        if (costLabel != null) {
            String[] args = getCostDescriptionArgs();
            Color[] colors = new Color[args.length];
            Arrays.fill(colors, Settings.POSITIVE_HIGHLIGHT_COLOR);
            colors[colors.length-1] = canAfford ? Settings.POSITIVE_HIGHLIGHT_COLOR : Settings.NEGATIVE_HIGHLIGHT_COLOR;
            costLabel.setText(String.format(getCostDescriptionFormat(), (Object[]) args));
            costLabel.setHighlight(args);
            costLabel.setHighlightColors(colors);
            costLabel.setOpacity(shouldShow ? 1f : 0f);
        }
        if (confirmButton != null) {
            confirmButton.setEnabled(canApplyEffects() && canAfford && (!isStoryOption || hasEnoughSP));
        }
        if (spLabel != null && isStoryOption) {
            float bxp = HullmodUtils.getBonusXPFraction(baseCost);
            String bxpStr = Utils.asPercent(bxp);
            if (bxp <= 0f) {
                spLabel.setText(String.format(Strings.Misc.requiresStoryPointNoBonus, Strings.Misc.storyPoint));
                spLabel.setHighlight(Strings.Misc.storyPoint);
                spLabel.setHighlightColor(hasEnoughSP ? Misc.getStoryOptionColor() : Settings.NEGATIVE_HIGHLIGHT_COLOR);
            } else {
                spLabel.setText(String.format(Strings.Misc.requiresStoryPointWithBonus, Strings.Misc.storyPoint, bxpStr));
                spLabel.setHighlight(Strings.Misc.storyPoint, bxpStr);
                spLabel.setHighlightColors(hasEnoughSP ? Misc.getStoryOptionColor() : Settings.NEGATIVE_HIGHLIGHT_COLOR, Misc.getStoryOptionColor());
            }
            spLabel.setOpacity(shouldShow ? 1f : 0f);
        }
    }

    protected abstract String getCostDescriptionFormat();
    protected abstract boolean canApplyEffects();
    protected boolean shouldShowCostLabels() {
        return true;
    }
    protected abstract String[] getCostDescriptionArgs();
    protected abstract String getUsedSPDescription();

    protected final void addCostLabels(CustomPanelAPI panel, float width, float padFromTop) {
        var costText = panel.createUIElement(width, 50f, false);
        costLabel = costText.addPara(getCostDescriptionFormat(), 10f, Settings.POSITIVE_HIGHLIGHT_COLOR, getCostDescriptionArgs());
        if (isStoryOption) {
            spLabel = Utils.addStoryPointUseInfo(costText, 1f);
        }
        updateLabels();
        panel.addUIElement(costText).inTMid(padFromTop);
    }

    protected abstract float getBaseCost();
    protected final float getModifiedCost() {
        var base = getBaseCost();
        return isStoryOption ? base * HullmodUtils.CREDITS_COST_MULT_SP : base;
    }
}
