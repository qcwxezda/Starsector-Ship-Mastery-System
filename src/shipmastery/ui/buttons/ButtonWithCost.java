package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.CampaignUtils;
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
    }

    protected final void applyCosts() {
        float cost = getModifiedCost();
        Utils.getPlayerCredits().subtract(cost);
        if (isStoryOption) {
            Global.getSector().getPlayerStats().spendStoryPoints(
                    1,
                    true,
                    null,
                    true,
                    getBxpFraction(),
                    getUsedSPDescription());
            Global.getSoundPlayer().playUISound(getUsedSPSound(), 1f, 1f);
        } else {
            Global.getSoundPlayer().playUISound(getNormalSound(), 1f, 1f);
        }
    }

    protected String getNormalSound() {
        return "sms_add_smod";
    }

    protected String getUsedSPSound() {
        return "ui_char_spent_story_point_industry";
    }

    protected void updateLabels() {
        float cost = getModifiedCost();
        boolean canAfford = cost <= Utils.getPlayerCredits().get();
        boolean hasEnoughSP = Global.getSector().getPlayerStats().getStoryPoints() >= 1;
        boolean shouldShowCost = shouldShowCostLabelNow();
        boolean shouldShowSPCost = shouldShowSPCostLabelNow();

        if (costLabel != null) {
            String[] args = getCostDescriptionArgs();
            Color[] colors = new Color[args.length];
            Arrays.fill(colors, Settings.POSITIVE_HIGHLIGHT_COLOR);
            if (colors.length > 0) {
                colors[colors.length - 1] = canAfford ? Settings.POSITIVE_HIGHLIGHT_COLOR : Settings.NEGATIVE_HIGHLIGHT_COLOR;
            }
            costLabel.setText(String.format(getCostDescriptionFormat(), (Object[]) args));
            costLabel.setHighlight(args);
            costLabel.setHighlightColors(colors);
            costLabel.setOpacity(shouldShowCost ? 1f : 0f);
        }
        if (confirmButton != null) {
            confirmButton.setEnabled(canApplyEffects() && canAfford && (!isStoryOption || hasEnoughSP));
        }
        if (spLabel != null && isStoryOption) {
            float bxp = getBxpFraction();
            String bxpStr = Utils.asPercent(bxp);
            int sp = Global.getSector().getPlayerStats().getStoryPoints();
            String pointOrPoints = sp == 1 ? Strings.Misc.storyPoint : Strings.Misc.storyPoints;
            if (bxp <= 0f) {
                spLabel.setText(String.format(
                        Strings.Misc.requiresStoryPointNoBonus,
                        Strings.Misc.storyPoint,
                        "" + sp,
                        pointOrPoints));
                spLabel.setHighlight(Strings.Misc.storyPoint, "" + sp, pointOrPoints);
                spLabel.setHighlightColor(hasEnoughSP ? Misc.getStoryOptionColor() : Settings.NEGATIVE_HIGHLIGHT_COLOR);
            } else {
                spLabel.setText(String.format(
                        Strings.Misc.requiresStoryPointWithBonus,
                        Strings.Misc.storyPoint,
                        bxpStr,
                        "" + sp,
                        pointOrPoints));
                spLabel.setHighlight(Strings.Misc.storyPoint, bxpStr, "" + sp, pointOrPoints);
                Color hc = hasEnoughSP ? Misc.getStoryOptionColor() : Settings.NEGATIVE_HIGHLIGHT_COLOR;
                spLabel.setHighlightColors(hc, Misc.getStoryOptionColor(), hc, hc);
            }
            spLabel.setOpacity(shouldShowSPCost ? 1f : 0f);
        }
    }

    protected abstract String getCostDescriptionFormat();
    protected abstract boolean canApplyEffects();
    protected boolean hasCostLabel() {
        return true;
    }
    protected boolean shouldShowCostLabelNow() {
        return true;
    }
    protected boolean shouldShowSPCostLabelNow() {
        return true;
    }
    protected float getBxpFraction() {
        return HullmodUtils.getBonusXPFraction(getBaseCost());
    }
    protected abstract String[] getCostDescriptionArgs();
    protected abstract String getUsedSPDescription();

    protected final TooltipMakerAPI addCostLabels(CustomPanelAPI panel, float width, float padFromTop) {
        var costText = panel.createUIElement(width, 50f, false);
        if (hasCostLabel()) {
            costLabel = costText.addPara(getCostDescriptionFormat(), 10f, Settings.POSITIVE_HIGHLIGHT_COLOR, getCostDescriptionArgs());
        } else {
            costText.addSpacer(13f);
        }
        if (isStoryOption) {
            spLabel = CampaignUtils.addStoryPointUseInfo(costText, 1f);
        }
        updateLabels();
        panel.addUIElement(costText).inTMid(padFromTop);
        return costText;
    }

    protected abstract float getBaseCost();
    protected final float getModifiedCost() {
        var base = getBaseCost();
        return isStoryOption ? base * HullmodUtils.CREDITS_COST_MULT_SP : base;
    }
}
