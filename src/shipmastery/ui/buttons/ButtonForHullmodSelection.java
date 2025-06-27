package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
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

public abstract class ButtonForHullmodSelection extends ButtonWithCost {

    protected final Set<String> selectedIds = new HashSet<>();
    protected final List<ButtonAPI> buttons = new ArrayList<>();
    protected PickerPanelPlugin plugin;
    protected final ShipAPI selectedShip;

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

    @Override
    protected boolean canApplyEffects() {
        return !selectedIds.isEmpty();
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
                            applyEffects();
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
        addCostLabels(panel, buttonListWidth, buttonListHeight + 10f);
        dialogData.panel.addComponent(panel).inTMid(45f);
    }

    protected abstract Collection<String> getEligibleHullmodIds();
    protected abstract String getTitle();
    protected abstract Color getButtonTextColor();
    protected abstract void applyEffects();

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
