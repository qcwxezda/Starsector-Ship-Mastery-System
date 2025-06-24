package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ShipMastery;
import shipmastery.deferred.Action;
import shipmastery.ui.triggers.DialogDismissedListener;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class LevelUpDialog {

    private final FleetMemberAPI member;
    private final Action onLevelUp;
    private String selectedLevelId;

    public LevelUpDialog(FleetMemberAPI member, Action onLevelUp) {
        this.member = member;
        this.onLevelUp = onLevelUp;
    }

    public void show() {

        float width = 800f;
        float displayW = width-100f;
        var spec = member.getHullSpec();
        int currentLevel = ShipMastery.getPlayerMasteryLevel(spec);

        // Have to create a temporary display to figure out the total height it uses
        CustomPanelAPI temp = Global.getSettings().createCustom(displayW, 100f, null);
        TooltipMakerAPI tempTTM = temp.createUIElement(displayW, 100f, true);
        var tempDisplay = new MasteryDisplay(
                null,
                member.getVariant(),
                member,
                false,
                displayW,
                100f,
                0f,
                false,
                () -> {});
        tempDisplay.create(tempTTM, currentLevel+1, currentLevel+1, true);

        float displayH = Math.min(500f, tempDisplay.getTotalHeight() + 5f);
        float height =  displayH + 100f;

        ReflectionUtils.GenericDialogData data = ReflectionUtils.showGenericDialog("", Strings.Misc.confirm, Strings.Misc.cancel, width, height, new DialogDismissedListener() {
            @Override
            public void trigger(Object... args) {
                if ((int) args[1] == 1 || selectedLevelId == null) return;
                var spec = member.getHullSpec();
                ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
                ShipMastery.advancePlayerMasteryLevel(spec);
                int level = ShipMastery.getPlayerMasteryLevel(spec);
                ShipMastery.activatePlayerMastery(member.getHullSpec(), level, selectedLevelId);
                Global.getSoundPlayer().playUISound("sms_increase_mastery", 1f, 1f);

                if (onLevelUp != null) {
                    onLevelUp.perform();
                }

                // Can level up again
                if (ShipMastery.getPlayerMasteryPoints(spec) >= MasteryUtils.getUpgradeCost(spec)) {
                    new LevelUpDialog(member, onLevelUp).show();
                }
            }
        });
        if (data == null) return;

        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, null);

        TooltipMakerAPI levelUpTitle = panel.createUIElement(width, height, false);
        levelUpTitle.setTitleFont(Fonts.ORBITRON_24AA);
        levelUpTitle.addTitle(String.format(Strings.MasteryPanel.levelUpSelect, Utils.getRestoredHullSpec(spec).getHullNameWithDashClass())).setAlignment(Alignment.MID);
        panel.addUIElement(levelUpTitle).inTR(30f, 25f);

        TooltipMakerAPI outline = panel.createUIElement(displayW, displayH, false);
        new MasteryDisplayOutline(displayW, displayH).create(outline);
        panel.addUIElement(outline).inTR(40f, 60f);

        TooltipMakerAPI display = panel.createUIElement(displayW+46f, displayH-5f, true);
        MasteryDisplay displayItem = new MasteryDisplay(
                null,
                member.getVariant(),
                member,
                false,
                displayW,
                displayH,
                0f,
                false,
                () -> {});

        displayItem.create(display, currentLevel+1, currentLevel+1, true);
        display.setHeightSoFar(displayItem.getTotalHeight());

        panel.addUIElement(display).inTR(39f, 60f);
        data.panel.addComponent(panel);
        ReflectionUtils.invokeMethod(display.getExternalScroller(), "setMaxShadowHeight", 0f);

        updateSelectedLevel(data.confirmButton, displayItem, currentLevel + 1);
        displayItem.setOnButtonClick(() -> updateSelectedLevel(data.confirmButton, displayItem, currentLevel + 1));
    }

    private void updateSelectedLevel(ButtonAPI confirmButton, MasteryDisplay display, int level) {
        selectedLevelId = display.selectedLevels.get(level);
        confirmButton.setEnabled(selectedLevelId != null);
    }
}
