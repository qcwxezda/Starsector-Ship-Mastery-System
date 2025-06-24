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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class ButtonForHullmodSelection extends ButtonWithIcon{

    protected final ShipAPI selectedShip;
    protected final Set<String> selectedIds = new HashSet<>();
    protected LabelAPI costLabel;
    protected LabelAPI spLabel;
    protected ButtonAPI confirmButton;

    public ButtonForHullmodSelection(String spriteName, boolean useStoryColors, ShipAPI selectedShip) {
        super(spriteName, useStoryColors);
        this.selectedShip = selectedShip;
    }

    private class PickerPanelPlugin extends BaseCustomUIPanelPlugin {
        @Override
        public void buttonPressed(Object buttonId) {
            var id = (String) buttonId;
            if (selectedIds.contains(id)) {
                selectedIds.remove(id);
            } else {
                selectedIds.add(id);
            }
            float baseCost = getBaseCost();
            float cost = getModifiedCost();
            if (costLabel != null) {
                var creditsText = Misc.getDGSCredits(cost);
                costLabel.setText(String.format(Strings.MasteryPanel.selectiveRestorationPanelText, creditsText));
                costLabel.setHighlight(creditsText);
            }
            if (confirmButton != null) {
                confirmButton.setEnabled(!selectedIds.isEmpty());
            }
            if (spLabel != null) {
                float bxp = HullmodUtils.getBonusXPFraction(baseCost);
                String bxpStr = Utils.asPercent(bxp);
                if (bxp <= 0f) {
                    spLabel.setText(String.format(Strings.Misc.requiresStoryPointNoBonus, Strings.Misc.storyPoint));
                    spLabel.setHighlight(Strings.Misc.storyPoint);
                } else {
                    spLabel.setText(String.format(Strings.Misc.requiresStoryPointWithBonus, Strings.Misc.storyPoint, bxpStr));
                    spLabel.setHighlight(Strings.Misc.storyPoint, bxpStr);
                }
            }
        }
    }

    @Override
    public void onClick() {
        selectedIds.clear();
        var hullmods = getEligibleHullmodIds();
        float buttonListWidth = 500f;
        int count = hullmods.size();
        float buttonHeight = 30f, buttonPad = 10f;
        float buttonListHeight = Math.min(500f, count * (buttonHeight + buttonPad));

        var dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.Misc.confirm,
                Strings.Misc.cancel,
                buttonListWidth+100f, buttonListHeight+155f + (useStoryColors ? 20f : 0f),
                new DialogDismissedListener() {
                    @Override
                    public void trigger(Object... args) {
                        if ((int) args[1] == 0 && confirmButton != null && confirmButton.isEnabled()) {
                            onConfirm();
                            finish();
                        }
                    }
                });
        if (dialogData == null) return;

        confirmButton = dialogData.confirmButton;
        confirmButton.setEnabled(false);
        if (useStoryColors) {
            ReflectionUtils.setButtonColor(confirmButton, Misc.getStoryDarkColor());
            ReflectionUtils.setButtonTextColor(confirmButton, Misc.getStoryOptionColor());
        }

        var panel = Global.getSettings().createCustom(buttonListWidth+100f, buttonListHeight+100f, new PickerPanelPlugin());
        var title = panel.createUIElement(buttonListWidth, 30f, false);
        title.setTitleFont(Fonts.ORBITRON_24AA);
        title.addTitle(getTitle()).setAlignment(Alignment.MID);
        panel.addUIElement(title).inTMid(-30f);

        var buttonsList = panel.createUIElement(buttonListWidth, buttonListHeight, true);
        hullmods.stream()
                .map(DModManager::getMod)
                .forEach(x -> {
                    buttonsList.addAreaCheckbox(
                            x.getDisplayName(),
                            x.getId(),
                            baseColor,
                            darkColor,
                            brightColor,
                            buttonListWidth - 5f,
                            buttonHeight,
                            buttonPad);
                    buttonsList.addTooltipToPrevious(
                            new HullmodUtils.HullmodTooltipCreator(x, selectedShip),
                            TooltipMakerAPI.TooltipLocation.ABOVE,
                            false);
                });
        panel.addUIElement(buttonsList).inTMid(0f);

        var costText = panel.createUIElement(buttonListWidth, 50f, false);
        costLabel = costText.addPara(getDescriptionFormat(), 10f, Settings.POSITIVE_HIGHLIGHT_COLOR, getDescriptionArgs());
        if (useStoryColors) {
            spLabel = Utils.addStoryPointUseInfo(costText, 1f);
        }
        panel.addUIElement(costText).inTMid(buttonListHeight + 10f);
        dialogData.panel.addComponent(panel).inTMid(45f);
    }

    protected abstract String getTitle();
    protected abstract String getDescriptionFormat();
    protected abstract void onConfirm();
    protected abstract String[] getDescriptionArgs();
    protected abstract Collection<String> getEligibleHullmodIds();
    protected abstract float getBaseCost();

    protected final float getModifiedCost() {
        var base = getBaseCost();
        return useStoryColors ? base * HullmodUtils.CREDITS_COST_MULT_SP : base;
    }

}
