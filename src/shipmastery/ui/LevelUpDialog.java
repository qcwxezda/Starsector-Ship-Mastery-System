package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.achievements.FullEnhance;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.deferred.Action;
import shipmastery.ui.triggers.DialogDismissedListener;
import shipmastery.util.CampaignUtils;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.Map;

import static shipmastery.mastery.MasteryEffect.MASTERY_STRENGTH_MOD_FOR;

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
        boolean isEnhance = currentLevel >= ShipMastery.getMaxMasteryLevel(spec);

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
        tempDisplay.create(tempTTM, currentLevel+1, currentLevel+1, true, false);

        float displayH = Math.min(500f, tempDisplay.getTotalHeight() + 5f);
        float height =  displayH + 110f + (isEnhance ? 25f : 0f);

        class DialogDismissed extends DialogDismissedListener {
            ButtonAPI confirmButton = null;
            void setConfirmButton(ButtonAPI confirmButton) {
                this.confirmButton = confirmButton;
            }
            @Override
            public void trigger(Object... args) {
                if ((int) args[1] == 1 || confirmButton == null || !confirmButton.isEnabled()) return;

                int level = ShipMastery.getPlayerMasteryLevel(spec);

                var spec = Utils.getRestoredHullSpec(member.getHullSpec());
                if (!isEnhance) {
                    ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
                    ShipMastery.advancePlayerMasteryLevel(spec);
                    ShipMastery.activatePlayerMastery(member.getHullSpec(), level+1, selectedLevelId);
                    Global.getSoundPlayer().playUISound("sms_increase_mastery", 1f, 1f);
                } else {
                    //noinspection unchecked
                    Map<String, Integer> enhanceMap = (Map<String, Integer>) Global.getSector().getPersistentData().get(MasteryUtils.ENHANCE_MAP);
                    if (enhanceMap == null) {
                        enhanceMap = new HashMap<>();
                        Global.getSector().getPersistentData().put(MasteryUtils.ENHANCE_MAP, enhanceMap);
                    }
                    Integer enhanceCount = enhanceMap.getOrDefault(spec.getHullId(), 0);

                    ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getEnhanceMPCost(spec));
                    int spCost = MasteryUtils.getEnhanceSPCost();
                    Global.getSector().getPlayerStats().spendStoryPoints(
                            spCost,
                            spCost > 0,
                            null,
                            spCost > 0,
                            MasteryUtils.getEnhanceBonusXP(spec),
                            null);

                    enhanceCount = enhanceCount + 1;
                    enhanceMap.put(spec.getHullId(), enhanceCount);


                    float enhanceAmount = 0f;
                    for (int i = 0; i < enhanceCount; i++) {
                        enhanceAmount += MasteryUtils.ENHANCE_MASTERY_AMOUNT[i];
                    }
                    Global.getSector().getPlayerStats().getDynamic().getMod(MASTERY_STRENGTH_MOD_FOR + spec.getHullId())
                            .modifyPercent(MasteryUtils.ENHANCE_MODIFIER_ID, 100f * enhanceAmount);

                    if (spCost > 0)
                        Global.getSoundPlayer().playUISound("ui_char_spent_story_point_technology", 1f, 1f);
                    else
                        Global.getSoundPlayer().playUISound("sms_increase_mastery", 1f, 1f);

                    // Check for achievement completion
                    if (enhanceCount == MasteryUtils.MAX_ENHANCES) {
                        UnlockAchievementAction.unlockWhenUnpaused(FullEnhance.class);
                    }
                }

                if (onLevelUp != null) {
                    onLevelUp.perform();
                }

                if (!isEnhance && ShipMastery.getPlayerMasteryLevel(spec) >= ShipMastery.getMaxMasteryLevel(spec)) return;
                if (isEnhance && MasteryUtils.getEnhanceCount(spec) == MasteryUtils.MAX_ENHANCES) return;
                // Can level up again
                if (ShipMastery.getPlayerMasteryPoints(spec) >= (isEnhance ? MasteryUtils.getEnhanceMPCost(spec) : MasteryUtils.getUpgradeCost(spec))) {
                    new LevelUpDialog(member, onLevelUp).show();
                }
            }
        }

        DialogDismissed listener = new DialogDismissed();
        ReflectionUtils.GenericDialogData data = ReflectionUtils.showGenericDialog("", Strings.Misc.confirm, Strings.Misc.cancel, width, height, listener);
        if (data == null) return;
        listener.setConfirmButton(data.confirmButton);

        if (isEnhance) {
            ReflectionUtils.setButtonTextColor(data.confirmButton, Misc.getStoryOptionColor());
            ReflectionUtils.setButtonColor(data.confirmButton, Misc.getStoryDarkColor());
        }

        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, null);

        TooltipMakerAPI levelUpTitle = panel.createUIElement(width, height, false);
        levelUpTitle.setTitleFont(Fonts.ORBITRON_24AA);
        if (isEnhance) {
            levelUpTitle.setTitleFontColor(Misc.getStoryOptionColor());
        }
        levelUpTitle.addTitle(String.format(isEnhance ? Strings.MasteryPanel.enhanceSelect :Strings.MasteryPanel.levelUpSelect, Utils.getRestoredHullSpec(spec).getHullNameWithDashClass())).setAlignment(Alignment.MID);
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

        displayItem.create(display, currentLevel+1, currentLevel+1, true, isEnhance);
        display.setHeightSoFar(displayItem.getTotalHeight());
        panel.addUIElement(display).inTR(39f, 60f);


        if (isEnhance) {
            var spInfo = panel.createUIElement(width, 20f, false);
            CampaignUtils.addStoryPointUseInfo(spInfo, MasteryUtils.getEnhanceBonusXP(spec));
            panel.addUIElement(spInfo).inBL(30f, 50f);
        }

        data.panel.addComponent(panel);
        ReflectionUtils.invokeMethod(display.getExternalScroller(), "setMaxShadowHeight", 0f);

        updateSelectedLevel(data.confirmButton, displayItem, currentLevel + 1, isEnhance);
        displayItem.setOnButtonClick(() -> updateSelectedLevel(data.confirmButton, displayItem, currentLevel + 1, isEnhance));
    }

    private void updateSelectedLevel(ButtonAPI confirmButton, MasteryDisplay display, int level, boolean isEnhance) {
        selectedLevelId = display.selectedLevels.get(level);
        confirmButton.setEnabled(selectedLevelId != null && (!isEnhance || Global.getSector().getPlayerStats().getStoryPoints() >= 1));
    }
}
