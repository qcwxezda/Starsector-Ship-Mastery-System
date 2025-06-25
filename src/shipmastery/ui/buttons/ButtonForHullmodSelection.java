package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.ui.triggers.DialogDismissedListener;
import shipmastery.util.HullmodUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ButtonForHullmodSelection extends ButtonWithIcon{

    protected final ShipAPI selectedShip;
    protected final Set<String> selectedIds = new HashSet<>();
    protected final List<ButtonAPI> buttons = new ArrayList<>();
    protected LabelAPI costLabel;
    protected LabelAPI spLabel;
    protected ButtonAPI confirmButton;
    protected PickerPanelPlugin plugin;

    public ButtonForHullmodSelection(String spriteName, boolean useStoryColors, ShipAPI selectedShip) {
        super(spriteName, useStoryColors);
        this.selectedShip = selectedShip;
    }

    protected class PickerPanelPlugin extends BaseCustomUIPanelPlugin {
        @Override
        public void buttonPressed(Object buttonId) {
            update();
        }

        public void update() {
            selectedIds.clear();
            selectedIds.addAll(
                    buttons.stream()
                            .filter(ButtonAPI::isChecked)
                            .map(button -> (String) button.getCustomData())
                            .toList());
            updateLabels();
        }
    }

    protected final void updateLabels() {
        float baseCost = getBaseCost();
        float cost = getModifiedCost();
        boolean canAfford = cost <= Utils.getPlayerCredits().get();
        boolean hasEnoughSP = Global.getSector().getPlayerStats().getStoryPoints() >= 1;

        if (costLabel != null) {
            costLabel.setText(String.format(getDescriptionFormat(), (Object[]) getDescriptionArgs()));
            costLabel.setHighlight(getDescriptionArgs());
            costLabel.setHighlightColor(canAfford ? Settings.POSITIVE_HIGHLIGHT_COLOR : Settings.NEGATIVE_HIGHLIGHT_COLOR);
        }
        if (confirmButton != null) {
            confirmButton.setEnabled(!selectedIds.isEmpty() && canAfford && (!isStoryOption || hasEnoughSP));
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
        }
    }

    @Override
    public void onClick() {
        selectedIds.clear();
        buttons.clear();

        var hullmods = getEligibleHullmodIds();
        float buttonListWidth = 500f;
        int count = hullmods.size();
        float buttonHeight = 30f, buttonPad = 10f;
        float buttonListHeight = Math.min(500f, count * (buttonHeight + buttonPad));

        var dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.Misc.confirm,
                Strings.Misc.cancel,
                buttonListWidth+100f, buttonListHeight+155f + (isStoryOption ? 20f : 0f),
                new DialogDismissedListener() {
                    @Override
                    public void trigger(Object... args) {
                        if ((int) args[1] == 0 && confirmButton != null && confirmButton.isEnabled()) {
                            onConfirm();
                            if (isStoryOption) {
                                Global.getSoundPlayer().playUISound("ui_char_spent_story_point_industry", 1f, 1f);
                            } else {
                                Global.getSoundPlayer().playUISound("sms_add_smod", 1f, 1f);
                            }
                            finish();
                        }
                    }
                });
        if (dialogData == null) return;

        confirmButton = dialogData.confirmButton;
        confirmButton.setEnabled(false);
        if (isStoryOption) {
            ReflectionUtils.setButtonColor(confirmButton, Misc.getStoryDarkColor());
            ReflectionUtils.setButtonTextColor(confirmButton, Misc.getStoryOptionColor());
        }

        plugin = new PickerPanelPlugin();
        var panel = Global.getSettings().createCustom(buttonListWidth+100f, buttonListHeight+100f, plugin);
        var title = panel.createUIElement(buttonListWidth, 30f, false);
        title.setTitleFont(Fonts.ORBITRON_24AA);
        title.setTitleFontColor(isStoryOption ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor());
        title.addTitle(getTitle()).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(-30f);

        TooltipMakerAPI selectAllTTM = panel.createUIElement(32f, 32f, false);
        var selectAll = new SelectAllButton();
        selectAll.create(selectAllTTM);
        panel.addUIElement(selectAllTTM).inTR(19f, -38f);

        var buttonColor = getButtonTextColor();
        var darkButtonColor = Utils.mixColor(buttonColor, Color.BLACK, 0.3f);
        var veryDarkButtonColor = Utils.mixColor(buttonColor, Color.BLACK, 0.6f);
        var buttonsList = panel.createUIElement(buttonListWidth, buttonListHeight, true);
        hullmods.stream()
                .map(DModManager::getMod)
                .forEach(x -> {
                    buttons.add(buttonsList.addAreaCheckbox(
                            x.getDisplayName(),
                            x.getId(),
                            darkButtonColor,
                            veryDarkButtonColor,
                            buttonColor,
                            buttonListWidth - 10f,
                            buttonHeight,
                            buttonPad));
                    buttonsList.addTooltipToPrevious(
                            new HullmodUtils.HullmodTooltipCreator(x, selectedShip),
                            TooltipMakerAPI.TooltipLocation.ABOVE,
                            false);
                });
        panel.addUIElement(buttonsList).inTMid(0f).setXAlignOffset(5f);

        var costText = panel.createUIElement(buttonListWidth, 50f, false);
        costLabel = costText.addPara(getDescriptionFormat(), 10f, Settings.POSITIVE_HIGHLIGHT_COLOR, getDescriptionArgs());
        if (isStoryOption) {
            spLabel = Utils.addStoryPointUseInfo(costText, 1f);
        }
        updateLabels();

        panel.addUIElement(costText).inTMid(buttonListHeight + 10f);
        dialogData.panel.addComponent(panel).inTMid(45f);
    }

    protected abstract String getTitle();
    protected abstract String getDescriptionFormat();
    protected abstract void onConfirm();
    protected abstract String[] getDescriptionArgs();
    protected abstract Collection<String> getEligibleHullmodIds();
    protected abstract float getBaseCost();
    protected abstract Color getButtonTextColor();

    protected final float getModifiedCost() {
        var base = getBaseCost();
        return isStoryOption ? base * HullmodUtils.CREDITS_COST_MULT_SP : base;
    }

    public class SelectAllButton extends ButtonWithIcon {
        public SelectAllButton() {
            super("graphics/icons/ui/sms_all_icon.png", false);
        }

        @Override
        public void onClick() {
            if (buttons.stream().allMatch(ButtonAPI::isChecked)) {
                buttons.forEach(x -> x.setChecked(false));
            } else {
                buttons.forEach(x -> x.setChecked(true));
            }
            if (plugin != null) {
                plugin.update();
            }
        }

        @Override
        public String getTooltipTitle() {
            return Strings.MasteryPanel.toggleAllButton;
        }

        @Override
        public float getTooltipWidth() {
            return 90f;
        }

        @Override
        public void appendToTooltip(TooltipMakerAPI tooltip) {}
    }


}
