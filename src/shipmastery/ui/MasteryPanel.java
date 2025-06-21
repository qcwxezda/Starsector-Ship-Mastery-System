package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.ui.UITable;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.campaign.RefitHandler;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.hullmods.EngineeringOverride;
import shipmastery.ui.triggers.CancelMasteryChangesPressed;
import shipmastery.ui.triggers.ClearSModsPressed;
import shipmastery.ui.triggers.ConfirmMasteryChangesPressed;
import shipmastery.ui.triggers.SModTableHeaderPressed;
import shipmastery.ui.triggers.SModTableRowPressed;
import shipmastery.ui.triggers.TabButtonPressed;
import shipmastery.ui.triggers.UseSPButtonPressed;
import shipmastery.util.ClassRefs;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MasteryPanel {
    ShipAPI root;
    ShipAPI module;
    RefitHandler handler;
    UIPanelAPI rootPanel;
    static final String tableFont = Fonts.INSIGNIA_LARGE;
    static final String checkboxFont = Fonts.ORBITRON_24AABOLD;
    public final static Float[] columnWidths = new Float[]{50f, 425f, 200f, 125f, 225f, 125f};
    public final static String[] columnNames =
            new String[]{
                    Strings.MasteryPanel.iconHeader,
                    Strings.MasteryPanel.hullmodHeader,
                    Strings.MasteryPanel.designTypeHeader,
                    Strings.MasteryPanel.ordnancePointsHeader,
                    Strings.MasteryPanel.creditsHeader,
                    Strings.MasteryPanel.modularHeader};
    public static final float tableEntryHeight = 38f;


    String currentColumnName = columnNames[5];
    Comparator<HullModSpecAPI> comparator = makeComparator(currentColumnName);
    UIPanelAPI sModPanel, masteryPanel;
    ButtonAPI sModButton, masteryButton;
    boolean isShowingMasteryPanel = false;
    boolean isInRestorableMarket = false;
    MasteryDisplay savedMasteryDisplay;
    TooltipMakerAPI upgradeMasteryDisplay, confirmOrCancelDisplay, createConstructDisplay, rerollMasteryDisplay, enhanceMasteryDisplay;
    int currentMastery, maxMastery;
    boolean hasLogisticBuiltIn = false, hasLogisticEnhanceBonus = false, usingSP = false;

    public MasteryPanel(RefitHandler handler) {

        ReflectionUtils.GenericDialogData dialogData =
                ReflectionUtils.showGenericDialog("", Strings.MasteryPanel.dismissWindow, null, 1200f, 700f, null);
        if (dialogData == null) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.MasteryPanel.cantOpenPanel, Settings.NEGATIVE_HIGHLIGHT_COLOR);
            return;
        }

        rootPanel = dialogData.panel;
        this.handler = handler;
        generateDialog(rootPanel, false, false, false);
    }

    public void forceRefresh(boolean shouldSync, boolean shouldSaveIfSynced, boolean useSavedScrollerLocation, boolean scrollToStart) {
        if (rootPanel == null) return;

        handler.injectRefitScreen(shouldSync, shouldSaveIfSynced);
        savedMasteryDisplay.saveScrollerHeight();

        generateDialog(rootPanel, true, useSavedScrollerLocation, scrollToStart);
    }

    public void togglePanelVisibility(ButtonAPI button) {
        if (button == sModButton) {
            sModPanel.setOpacity(1f);
            masteryPanel.setOpacity(0f);
            isShowingMasteryPanel = false;
            masteryButton.setChecked(false);
            sModButton.setChecked(true);
            masteryButton.setShortcut(Keyboard.KEY_Q, false);
            sModButton.setShortcut(-1, false);
        } else if (button == masteryButton) {
            sModPanel.setOpacity(0f);
            masteryPanel.setOpacity(1f);
            isShowingMasteryPanel = true;
            sModButton.setChecked(false);
            masteryButton.setChecked(true);
            sModButton.setShortcut(Keyboard.KEY_Q, false);
            masteryButton.setShortcut(-1, false);
        }
    }

    void generateDialog(UIPanelAPI panel, boolean isRefresh, boolean useSavedScrollerLocation, boolean scrollToStart) {
        Pair<ShipAPI, ShipAPI> moduleAndShip = handler.getSelectedShip(ReflectionUtils.getCoreUI());
        module = moduleAndShip.one;
        root = moduleAndShip.two;
        if (root == null || module == null) {
            return;
        }

        isInRestorableMarket = ReflectionUtils.isInRestorableMarket(ReflectionUtils.getCoreUI());
        if (isRefresh) {
            List<?> children = (List<?>) ReflectionUtils.invokeMethod(panel, "getChildrenNonCopy");

            // Only remove the UIPanels -- the first 2 children are a label and the confirm button,
            // which don't need to be refreshed
            children.removeIf(o -> o instanceof UIPanelAPI);
        }

        float w = panel.getPosition().getWidth() + 20f, h = panel.getPosition().getHeight();
        UIPanelAPI tabButtons = makeTabButtons(120f, 40f);
        UIPanelAPI currencyPanel = makeCurrencyLabels(w);
        sModPanel = makeThisShipPanel(w, h - 100f);
        masteryPanel = makeMasteryPanel(w, h - 100f, useSavedScrollerLocation, scrollToStart);
        togglePanelVisibility(!isInRestorableMarket || isShowingMasteryPanel ? masteryButton : sModButton);

        panel.addComponent(tabButtons).inTMid(0f);
        panel.addComponent(sModPanel).belowMid(tabButtons, 10f);
        panel.addComponent(masteryPanel).belowMid(tabButtons, 10f);
        panel.addComponent(currencyPanel).inBMid(0f);
    }

    @SuppressWarnings("SameParameterValue")
    UIPanelAPI makeTabButtons(float w, float h) {
        float pad = 10f;

        TabButtonPressed tabButtonListener = new TabButtonPressed(this);
        CustomPanelAPI tabsPanel = Global.getSettings().createCustom(2 * w + pad, h, null);
        TooltipMakerAPI thisShipTab = tabsPanel.createUIElement(w, h, false);
        thisShipTab.setAreaCheckboxFont(checkboxFont);
        sModButton = thisShipTab.addAreaCheckbox(Strings.MasteryPanel.hullmodsTab, null, Misc.getBasePlayerColor(),
                                                 Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w, h, 0f);

        ReflectionUtils.setButtonListener(sModButton, tabButtonListener);
        thisShipTab.setAreaCheckboxFontDefault();

        if (!isInRestorableMarket) {
            sModButton.setEnabled(false);
            thisShipTab.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                @Override
                public boolean isTooltipExpandable(Object o) {
                    return false;
                }

                @Override
                public float getTooltipWidth(Object o) {
                    return 300f;
                }

                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    tooltip.setParaSmallInsignia();
                    tooltip.addPara(Strings.MasteryPanel.mustBeDockedHullmods, 0f);
                }
            }, TooltipMakerAPI.TooltipLocation.BELOW, false);
        }

        TooltipMakerAPI hullTypeTab = tabsPanel.createUIElement(w, h, false);
        hullTypeTab.setAreaCheckboxFont(checkboxFont);
        masteryButton =
                hullTypeTab.addAreaCheckbox(Strings.MasteryPanel.masteryTab, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                            Misc.getBrightPlayerColor(), w, h, 0f);

        ReflectionUtils.setButtonListener(masteryButton, tabButtonListener);
        hullTypeTab.setAreaCheckboxFontDefault();

        tabsPanel.addUIElement(thisShipTab).inTL(-10f, 10f);
        tabsPanel.addUIElement(hullTypeTab).rightOfMid(thisShipTab, 10f);

        return tabsPanel;
    }

    UIPanelAPI makeCurrencyLabels(float width) {
        CustomPanelAPI labelsPanel = Global.getSettings().createCustom(width, 50f, null);

        int creditsAmt = (int) Utils.getPlayerCredits().get();
        String creditsAmtFmt = Misc.getFormat().format(creditsAmt);
        String creditsString = Strings.MasteryPanel.creditsDisplay + creditsAmtFmt;
        float creditsStringWidth =
                Global.getSettings().computeStringWidth(creditsString + 10f, "graphics/fonts/orbitron20aabold.fnt");

        TooltipMakerAPI credits = labelsPanel.createUIElement(creditsStringWidth, 30f, false);
        credits.setParaOrbitronLarge();
        LabelAPI creditsLabel = credits.addPara(creditsString, 10f);
        creditsLabel.setAlignment(Alignment.LMID);
        creditsLabel.setHighlight(creditsAmtFmt);
        creditsLabel.setHighlightColor(Misc.getHighlightColor());

        int masteryPointsAmt = (int) ShipMastery.getPlayerMasteryPoints(root.getHullSpec());
        String masteryPointsString = Strings.MasteryPanel.masteryPointsDisplay + masteryPointsAmt;
        float masteryPointsStringWidth = 10f + Global.getSettings().computeStringWidth(masteryPointsString,
                                                                                 "graphics/fonts/orbitron20aabold.fnt");
        TooltipMakerAPI masteryPoints = labelsPanel.createUIElement(masteryPointsStringWidth, 30f, false);
        masteryPoints.setParaOrbitronLarge();
        LabelAPI masteryPointsLabel = masteryPoints.addPara(masteryPointsString, 10f);
        masteryPointsLabel.setAlignment(Alignment.LMID);
        masteryPointsLabel.setHighlight("" + masteryPointsAmt);
        masteryPointsLabel.setHighlightColor(Settings.MASTERY_COLOR);

        int storyPointsAmt = Global.getSector().getPlayerStats().getStoryPoints();
        String storyPointsString = Strings.MasteryPanel.storyPointsDisplay + storyPointsAmt;
        float storyPointsStringWidth = 10f + Global.getSettings().computeStringWidth(storyPointsString,
                "graphics/fonts/orbitron20aabold.fnt");
        TooltipMakerAPI storyPoints = labelsPanel.createUIElement(storyPointsStringWidth, 30f, false);
        storyPoints.setParaOrbitronLarge();
        LabelAPI storyPointsLabel = storyPoints.addPara(storyPointsString, 10f);
        storyPointsLabel.setAlignment(Alignment.LMID);
        storyPointsLabel.setHighlight("" + storyPointsAmt);
        storyPointsLabel.setHighlightColor(Misc.getStoryBrightColor());

        labelsPanel.addUIElement(credits).inBL(20f, 10f);
        labelsPanel.addUIElement(masteryPoints).inBL(380f, 10f);
        labelsPanel.addUIElement(storyPoints).inBL(810f, 10f);
        return labelsPanel;
    }

    UIPanelAPI makeThisShipPanel(float width, float height) {
        ShipVariantAPI moduleVariant = module.getVariant();
        hasLogisticBuiltIn = SModUtils.hasLogisticSMod(moduleVariant);
        hasLogisticEnhanceBonus = SModUtils.hasBonusLogisticSlot(moduleVariant);

        List<HullModSpecAPI> applicableSpecs = new ArrayList<>();
        for (String id : moduleVariant.getHullSpec().getBuiltInMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            HullModEffect effect = spec.getEffect();
            if (effect.hasSModEffect() && !effect.isSModEffectAPenalty() &&
                    !moduleVariant.getSModdedBuiltIns().contains(id)) {
                applicableSpecs.add(spec);
            }
        }
        for (String id : moduleVariant.getNonBuiltInHullmods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (!spec.isHidden() && !spec.isHiddenEverywhere()) {
                applicableSpecs.add(spec);
            }
        }
        applicableSpecs.sort(comparator);

        CustomPanelAPI thisShipPanel = Global.getSettings().createCustom(width, height, null);
        Object[] columnData = Utils.interleaveArrays(columnNames, columnWidths);

        TooltipMakerAPI buildInListHeader = thisShipPanel.createUIElement(width - 25f, 25f, false);
        UITable headerTable =
                (UITable) buildInListHeader.beginTable(
                        usingSP ? Misc.getStoryOptionColor() : Misc.getBasePlayerColor(),
                        usingSP ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(),
                        usingSP ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor(),
                        tableEntryHeight,
                        false,
                        true,
                        columnData);
        Object header = ReflectionUtils.invokeMethod(headerTable, "getHeader");
        List<?> headerChildren = (List<?>) ReflectionUtils.invokeMethod(header, "getChildrenNonCopy");
        for (int i = 0; i < headerChildren.size(); i++) {
            Object child = headerChildren.get(i);

            if (!(child instanceof ButtonAPI headerButton)) continue;
            if (columnNames[i].equals(currentColumnName)) {
                headerButton.highlight();
            }
            ReflectionUtils.setButtonListener(headerButton, new SModTableHeaderPressed(this, i));
        }
        buildInListHeader.addTable("", -1, 0f);

        TooltipMakerAPI buildInList = thisShipPanel.createUIElement(width - 25f, height - 75f, true);
        UITable table = (UITable) buildInList.beginTable(
                usingSP ? Misc.getStoryOptionColor() : Misc.getBasePlayerColor(),
                usingSP ? Misc.getStoryDarkColor() : Misc.getDarkPlayerColor(),
                usingSP ? Misc.getStoryBrightColor() : Misc.getBrightPlayerColor(),
                tableEntryHeight,
                true,
                false,
                columnData);
        ReflectionUtils.invokeMethodExtWithClasses(table, "setRowClickDelegate", false,
                                                   new Class[]{ClassRefs.uiTableDelegateClass},
                                                   new SModTableRowPressed(this, module, root).getProxy());

        buildInList.addSpacer(7f);

        for (HullModSpecAPI spec : applicableSpecs) {
            addRowToHullModTable(buildInList, table, spec, !SModUtils.isHullmodBuiltIn(spec, moduleVariant));
            buildInList.addImage(spec.getSpriteName(), 50f, tableEntryHeight - 6f, 6f);
        }

        buildInList.addTable(Strings.MasteryPanel.hullmodListsEmptyHint, -1, -buildInList.getHeightSoFar() + 10f);
        if (table.getRows().size() < 13) {
            table.autoSizeToRows(13);
        }

        float resetButtonW = 150f, resetButtonH = 30f;
        TooltipMakerAPI resetButtonTTM = thisShipPanel.createUIElement(resetButtonW, resetButtonH, false);
        resetButtonTTM.setButtonFontOrbitron20();
        ButtonAPI resetButton =
                resetButtonTTM.addButton(Strings.MasteryPanel.clearButton, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                         Alignment.MID, CutStyle.TL_BR, resetButtonW, resetButtonH, 0f);
        ReflectionUtils.setButtonListener(resetButton, new ClearSModsPressed(this, module, root, Strings.MasteryPanel.clearButton));
        if (moduleVariant.getSMods().isEmpty()) {
            resetButton.setEnabled(false);
        }
        if (!TransientSettings.SMOD_REMOVAL_ENABLED && !Settings.CLEAR_SMODS_ALWAYS_ENABLED) {
            resetButton.setOpacity(0f);
        }

        float useSPButtonW = 200f, useSPButtonH = 30f;
        TooltipMakerAPI useSPButtonTTM = thisShipPanel.createUIElement(useSPButtonW, useSPButtonH, false);
        useSPButtonTTM.setAreaCheckboxFont(Fonts.ORBITRON_20AABOLD);
        ButtonAPI useSPButton = useSPButtonTTM.addAreaCheckbox(Strings.MasteryPanel.useSPButton, null, Misc.getStoryOptionColor(), Misc.getStoryDarkColor(),
                Misc.getStoryBrightColor(), useSPButtonW, useSPButtonH, 0f);
        useSPButtonTTM.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return 425f;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addPara(
                        Strings.MasteryPanel.useSPHint,
                        0f,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Utils.asPercentNoDecimal(1f-SModUtils.CREDITS_COST_MULT_SP),
                        Misc.getDGSCredits(SModTableRowPressed.CREDITS_FOR_NO_BONUS_XP),
                        "" + 1,
                        "" + (1+EngineeringOverride.NUM_ADDITIONAL_SMODS),
                        Global.getSettings().getHullModSpec(Strings.Hullmods.ENGINEERING_OVERRIDE).getDisplayName());
            }
        }, TooltipMakerAPI.TooltipLocation.ABOVE);
        ReflectionUtils.setButtonListener(useSPButton, new UseSPButtonPressed(this));
        useSPButton.setChecked(isUsingSP());

        float modularCountW = 200f, modularCountH = 40f;
        int nSMods = moduleVariant.getSMods().size();


        var limit = getSModLimit(module);
        int sModLimit = limit.one;
        boolean limitEnhancedBySP = limit.two;

        //if (hasLogisticBuiltIn && hasLogisticEnhanceBonus) sModLimit++;
        TooltipMakerAPI modularCountTTM = thisShipPanel.createUIElement(modularCountW, modularCountH, false);
        modularCountTTM.setParaOrbitronVeryLarge();
        LabelAPI modularCount = modularCountTTM.addPara(
                Strings.MasteryPanel.builtInDisplay + String.format("%s/%s", nSMods, sModLimit),
                Misc.getBrightPlayerColor(), 0f);
        modularCount.setAlignment(Alignment.RMID);
        modularCount.setHighlight("" + nSMods, "" + sModLimit);
        modularCount.setHighlightColors(Misc.getHighlightColor(),
                hasLogisticBuiltIn && hasLogisticEnhanceBonus || limitEnhancedBySP ?
                        Misc.getStoryBrightColor() :
                        Misc.getHighlightColor());

        float hintTextW = 200f, hintTextH = 40f;
        TooltipMakerAPI hintTextTTM = thisShipPanel.createUIElement(hintTextW, hintTextH, false);
        hintTextTTM.addPara(Strings.MasteryPanel.doubleClickHint, Misc.getBasePlayerColor(), 0f);

        thisShipPanel.addUIElement(buildInList).inTMid(37f);
        thisShipPanel.addUIElement(buildInListHeader).inTMid(20f);

        thisShipPanel.addUIElement(resetButtonTTM).inTR(30f, -20f);
        thisShipPanel.addUIElement(useSPButtonTTM).inTL(13f, -20f);

        thisShipPanel.addUIElement(hintTextTTM).inBL(20f, -2f);
        thisShipPanel.addUIElement(modularCountTTM).inBR(20f, -10f);
        return thisShipPanel;
    }

    public Pair<Integer, Boolean> getSModLimit(ShipAPI ship) {
        int sModLimit = Misc.getMaxPermanentMods(ship);
        if (ship.getVariant() == null) return new Pair<>(sModLimit, false);
        boolean limitEnhancedBySP = false;
        if (usingSP) {
            if (sModLimit < 1) {
                sModLimit = 1;
                limitEnhancedBySP = true;
            }
            if (module.getVariant().hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE) && sModLimit < 1 + EngineeringOverride.NUM_ADDITIONAL_SMODS) {
                sModLimit = 1 + EngineeringOverride.NUM_ADDITIONAL_SMODS;
                limitEnhancedBySP = true;
            }
        }
        return new Pair<>(sModLimit, limitEnhancedBySP);
    }

    void showUpgradeOrConfirmation(boolean canEnhance) {
        if (Objects.equals(savedMasteryDisplay.getActiveLevels(), savedMasteryDisplay.getSelectedLevels())) {
            upgradeMasteryDisplay.setOpacity(currentMastery >= maxMastery ? 0f : 1f);
            createConstructDisplay.setOpacity(1f);
            rerollMasteryDisplay.setOpacity(currentMastery >= maxMastery ? 1f : 0f);
            enhanceMasteryDisplay.setOpacity(currentMastery >= maxMastery && canEnhance ? 1f : 0f);
            confirmOrCancelDisplay.setOpacity(0f);
        }
        else {
            upgradeMasteryDisplay.setOpacity(0f);
            createConstructDisplay.setOpacity(0f);
            rerollMasteryDisplay.setOpacity(0f);
            enhanceMasteryDisplay.setOpacity(0f);
            confirmOrCancelDisplay.setOpacity(1f);
        }
    }

    public Map<Integer, String> getSelectedMasteryButtons() {
        return savedMasteryDisplay == null ? new HashMap<>() : savedMasteryDisplay.getSelectedLevels();
    }

    UIPanelAPI makeMasteryPanel(float width, float height, boolean useSavedScrollerLocation, boolean scrollToStart) {
        final ShipHullSpecAPI restoredHullSpec = Utils.getRestoredHullSpec(root.getHullSpec());
        currentMastery = ShipMastery.getPlayerMasteryLevel(restoredHullSpec);
        maxMastery = ShipMastery.getMaxMasteryLevel(restoredHullSpec);

        CustomPanelAPI masteryPanel = Global.getSettings().createCustom(width, height, null);

        if (Settings.ENABLE_COPY_SEED_BUTTON) {
            TooltipMakerAPI copySeedButton = masteryPanel.createUIElement(100f, 50f, false);
            new CopySeedButton().create(copySeedButton);
            masteryPanel.addUIElement(copySeedButton).inTL(15f, -45f);
        }

        float shipDisplaySize = 250f;
        TooltipMakerAPI shipDisplay = masteryPanel.createUIElement(shipDisplaySize, shipDisplaySize + 25f, false);
        new ShipDisplay(restoredHullSpec, shipDisplaySize).create(shipDisplay);
        masteryPanel.addUIElement(shipDisplay).inTL(50f, 90f);

        upgradeMasteryDisplay = masteryPanel.createUIElement(200f, 100f, false);
        new UpgradeMasteryDisplay(this, restoredHullSpec).create(upgradeMasteryDisplay);
        masteryPanel.addUIElement(upgradeMasteryDisplay).belowMid(shipDisplay, 30f);

        createConstructDisplay = masteryPanel.createUIElement(200f, 100f, false);
        new CreateConstructDisplay(this, restoredHullSpec).create(createConstructDisplay);
        masteryPanel.addUIElement(createConstructDisplay).belowMid(shipDisplay, currentMastery >= maxMastery ? 0f : 95f);

        rerollMasteryDisplay = masteryPanel.createUIElement(200f, 100f, false);
        new RerollMasteryDisplay(this, restoredHullSpec).create(rerollMasteryDisplay);
        masteryPanel.addUIElement(rerollMasteryDisplay).belowMid(shipDisplay, 65f);

        final boolean canEnhance = MasteryUtils.getEnhanceCount(restoredHullSpec) < MasteryUtils.MAX_ENHANCES;

        enhanceMasteryDisplay = masteryPanel.createUIElement(200f, 100f, false);
        new EnhanceMasteryDisplay(this, restoredHullSpec).create(enhanceMasteryDisplay);
        masteryPanel.addUIElement(enhanceMasteryDisplay).belowMid(shipDisplay, 130f);

        if (currentMastery >= maxMastery) {
            upgradeMasteryDisplay.setOpacity(0f);
            createConstructDisplay.setOpacity(1f);
            rerollMasteryDisplay.setOpacity(1f);
            enhanceMasteryDisplay.setOpacity(canEnhance ? 1f : 0f);
        } else {
            rerollMasteryDisplay.setOpacity(0f);
            createConstructDisplay.setOpacity(1f);
            enhanceMasteryDisplay.setOpacity(0f);
            upgradeMasteryDisplay.setOpacity(1f);
        }

        confirmOrCancelDisplay = masteryPanel.createUIElement(225f, 100f, false);
        new ConfirmOrCancelDisplay(new ConfirmMasteryChangesPressed(this, root.getHullSpec()), new CancelMasteryChangesPressed(this)).create(confirmOrCancelDisplay);
        masteryPanel.addUIElement(confirmOrCancelDisplay).belowMid(shipDisplay, 10f);

        confirmOrCancelDisplay.setOpacity(0f);

        float containerW = 800f, containerH = height - 66f;
        TooltipMakerAPI masteryContainer = masteryPanel.createUIElement(containerW, containerH+2f, false);
        new MasteryDisplayOutline(containerW, containerH+2f).create(masteryContainer);
        masteryPanel.addUIElement(masteryContainer).inTR(51f, 17f);

        float containerPadX = 4f, containerPadY = 8f;
        float masteryDisplayWidth = containerW + 50f - containerPadX, masteryDisplayHeight = containerH + 2f - containerPadY;
        float pad = 120f;

        MasteryDisplay display = new MasteryDisplay(this, module, root, containerW, containerH, pad, !useSavedScrollerLocation, () -> showUpgradeOrConfirmation(canEnhance));
        CustomPanelAPI masteryDisplayPanel = masteryPanel.createCustomPanel(masteryDisplayWidth, masteryDisplayHeight, display.new MasteryDisplayPlugin());
        TooltipMakerAPI masteryDisplayTTM =
                masteryDisplayPanel.createUIElement(masteryDisplayWidth, masteryDisplayHeight, true);

        display.create(masteryDisplayTTM);
        masteryDisplayTTM.setHeightSoFar(display.getTotalHeight() - pad);
        masteryDisplayPanel.addUIElement(masteryDisplayTTM).inTR(50f, 18f);
        masteryPanel.addComponent(masteryDisplayPanel).inTR(0f, 0f);

        // No top/bottom shadows
        ReflectionUtils.invokeMethod(masteryDisplayTTM.getExternalScroller(), "setMaxShadowHeight", 0f);

        if (savedMasteryDisplay != null) {
            display.scrollToHeight(Math.max(0f, Math.min(savedMasteryDisplay.getSavedScrollerHeight(), display.getTotalHeight()-containerH-pad+6f)));
        }
        if (!useSavedScrollerLocation) {
            if (scrollToStart) {
                display.scrollToLevel(1, savedMasteryDisplay == null);
            }
            else {
                display.scrollToLevel(display.getLevelToScrollTo(), savedMasteryDisplay == null);
            }
        }

        savedMasteryDisplay = display;

        float buttonW = 40f, buttonH = 35f, buttonPad = 25f;
        int maxButtons = (int) (containerW / (buttonW + buttonPad));
        int buttonsPerRow;
        int numButtons;
        if (maxButtons < maxMastery) {
            buttonW = 28f; buttonH = 24f; buttonPad = 4f;
            buttonsPerRow = Math.min(maxMastery, (int) (containerW / (buttonW + buttonPad)));
            numButtons = Math.min(maxMastery, 2 * buttonsPerRow);
        } else {
            numButtons = buttonsPerRow = maxMastery;
        }

        float buttonsW = buttonsPerRow * (buttonW + buttonPad);
        var masteryButtons = new MasteryDisplayLevelButtons(display, restoredHullSpec, numButtons, buttonsPerRow, buttonW, buttonH, buttonPad);
        CustomPanelAPI masteryButtonsPanel = masteryPanel.createCustomPanel(containerW, buttonH, masteryButtons);
        TooltipMakerAPI levelButtonsTTM = masteryButtonsPanel.createUIElement(buttonsW, buttonH, false);
        masteryButtons.create(levelButtonsTTM);
        masteryButtonsPanel.addUIElement(levelButtonsTTM).inTMid(0f);
        masteryPanel.addComponent(masteryButtonsPanel).inTR(54f-buttonPad/2f,containerH + 25f + buttonH);

        int maxLevel = ShipMastery.getPlayerMasteryLevel(root.getHullSpec());
        Set<Integer> assignedLevels = ShipMastery.getPlayerActiveMasteriesCopy(root.getHullSpec()).keySet();
        int unassignedLevels = 0;
        for (int i = 1; i <= maxLevel; i++) {
            if (!assignedLevels.contains(i)) {
                unassignedLevels++;
            }
        }

        if (unassignedLevels > 0) {
            TooltipMakerAPI unassignedWarning = masteryPanel.createUIElement(300f, 20f, false);
            String warningText = unassignedLevels == 1 ? Strings.MasteryPanel.unassignedWarningTextSingular : Strings.MasteryPanel.unassignedWarningTextPlural;
            String warningTextFmt = String.format(warningText, unassignedLevels);
            LabelAPI warningLabel = unassignedWarning.addPara(warningText, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, "" + unassignedLevels);
            warningLabel.setColor(Misc.getGrayColor());
            masteryPanel.addUIElement(unassignedWarning).inTR(-240f + warningLabel.computeTextWidth(warningTextFmt), -10f);
        }

        return masteryPanel;
    }

    LabelAPI label(String str, Color color) {
        LabelAPI label = Global.getSettings().createLabel(str, MasteryPanel.tableFont);
        label.setColor(color);
        return label;
    }

    void addRowToHullModTable(TooltipMakerAPI tableTTM, UITable table, final HullModSpecAPI spec, boolean modular) {
        String name = Utils.shortenText(spec.getDisplayName(), tableFont, columnWidths[1]);
        String designType = Utils.shortenText(spec.getManufacturer(), Fonts.DEFAULT_SMALL, columnWidths[2]);
        Color nameColor = modular ? Misc.getBrightPlayerColor() : Color.WHITE;
        Color designColor = Misc.getGrayColor();
        String opCost = "" + (modular ? spec.getCostFor(module.getHullSize()) : 0);
        int creditsCost = SModUtils.getCreditsCost(spec, module, usingSP);
        String creditsCostStr = Misc.getFormat().format(creditsCost);
        String modularString = modular ? Strings.MasteryPanel.yes : Strings.MasteryPanel.no;
        Color creditsColor = Misc.getHighlightColor();
        String cantBuildInReason = getCantBuildInReason(spec, modular, creditsCost);

        if (cantBuildInReason != null) {
            nameColor = creditsColor = Misc.getGrayColor();
        }

        boolean isExtraLogistics = cantBuildInReason == null && modular && !hasLogisticBuiltIn && hasLogisticEnhanceBonus && spec.hasUITag(HullMods.TAG_UI_LOGISTICS);
        tableTTM.addRowWithGlow(Alignment.MID, nameColor, " ", Alignment.LMID, nameColor, label(name, isExtraLogistics ? Misc.getStoryBrightColor() : nameColor),
                                Alignment.MID, designColor, designType, Alignment.MID, designColor, opCost, Alignment.MID,
                                Misc.getHighlightColor(), label(creditsCostStr, creditsColor), Alignment.MID, nameColor,
                                label(modularString, nameColor));

        tableTTM.addTooltipToAddedRow(new TooltipMakerAPI.TooltipCreator() {
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                HullModEffect effect = spec.getEffect();
                ShipAPI.HullSize hullSize = module.getHullSize();
                tooltip.addTitle(spec.getDisplayName());
                tooltip.addSpacer(10f);
                if (effect.shouldAddDescriptionToTooltip(hullSize, module, false)) {
                    List<String> highlights = new ArrayList<>();
                    String descParam;
                    // hard cap at 100 just in case getDescriptionParam for some reason
                    // doesn't default to null
                    for (int i = 0; i < 100 && (descParam = effect.getDescriptionParam(i, hullSize, module)) != null;
                         i++) {
                        highlights.add(descParam);
                    }
                    tooltip.addPara(spec.getDescription(hullSize).replaceAll("%", "%%"), 0f, Misc.getHighlightColor(),
                                    highlights.toArray(new String[0]));
                }
                effect.addPostDescriptionSection(tooltip, hullSize, module, getTooltipWidth(tooltipParam), true);
                if (effect.hasSModEffectSection(hullSize, module, false)) {
                    effect.addSModSection(tooltip, hullSize, module, getTooltipWidth(tooltipParam), false, true);
                }
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return 500f;
            }

            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }
        }, TooltipMakerAPI.TooltipLocation.RIGHT, false);

        TableRowData rowData = new TableRowData(spec.getId(), creditsCost, cantBuildInReason, modular);
        List<?> rows = (List<?>) ReflectionUtils.invokeMethod(table, "getRows");
        Object lastRow = rows.get(rows.size() - 1);
        ReflectionUtils.invokeMethodExtWithClasses(
                lastRow, "setData", false, new Class[]{Object.class}, rowData);
    }

    /**
     * Gives reason the mod can't be built in; returns null if hullmod can be built in
     */
    @Nullable String getCantBuildInReason(HullModSpecAPI spec, boolean modular, int creditsCost) {
        if (spec.hasTag(Tags.HULLMOD_NO_BUILD_IN) && !TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.contains(spec.getId())) {
            return spec.getDisplayName() + Strings.MasteryPanel.cantBuildIn;
        }
        if (Global.getSettings().isDevMode()) return null;

        int logisticsEnhanceBonus = hasLogisticEnhanceBonus && !hasLogisticBuiltIn && spec.hasUITag(HullMods.TAG_UI_LOGISTICS) ? 1 : 0;
        if (module.getVariant().getSMods().size() >= getSModLimit(module).one
                + TransientSettings.OVER_LIMIT_SMOD_COUNT.getModifiedInt() + logisticsEnhanceBonus && modular) {
            return Strings.MasteryPanel.limitReached;
        }

        int credits = (int) Utils.getPlayerCredits().get();
        int sp = Global.getSector().getPlayerStats().getStoryPoints();

        String notEnoughCredits = Strings.MasteryPanel.notEnoughCredits;
        String notEnoughStoryPoints = Strings.MasteryPanel.notEnoughStoryPoints;

        StringBuilder sb = new StringBuilder();
        if (sp < 1 && usingSP) sb.append(notEnoughStoryPoints);
        if (creditsCost > credits) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(notEnoughCredits);
        }

        if (!sb.isEmpty()) return sb.toString();
        return null;
    }

    public static class TableRowData {
        public final String hullModSpecId;
        public final int creditsCost;
        public final String cantBuildInReason;
        public final boolean isModular;

        // Can be built in <==> cantBuildInReason == null
        public TableRowData(String id, int credits, @Nullable String cantBuildInReason, boolean isModular) {
            hullModSpecId = id;
            creditsCost = credits;
            this.cantBuildInReason = cantBuildInReason;
            this.isModular = isModular;
        }
    }

    Object extractHullmodData(HullModSpecAPI spec, String columnName) {

        if (columnNames[0].equals(columnName)) {
            return spec.getDisplayName();
        }

        if (columnNames[1].equals(columnName)) {
            return spec.getDisplayName();
        }

        if (columnNames[2].equals(columnName)) {
            return spec.getManufacturer();
        }

        if (columnNames[3].equals(columnName)) {
            return spec.getCostFor(module.getHullSize());
        }

        if (columnNames[4].equals(columnName)) {
            return SModUtils.getCreditsCost(spec, module);
        }

        if (columnNames[5].equals(columnName)) {
            return !SModUtils.isHullmodBuiltIn(spec, module.getVariant());
        }

        return null;
    }

    Comparator<HullModSpecAPI> makeComparator(final String columnName) {
        return (spec1, spec2) -> {
            Object data1 = extractHullmodData(spec1, columnName);
            Object data2 = extractHullmodData(spec2, columnName);

            if (data1 == null || data2 == null) return 0;

            if (data1 instanceof String) {
                return ((String) data1).compareTo((String) data2);
            }
            if (data1 instanceof Integer) {
                return Integer.compare((int) data1, (int) data2);
            }
            if (data1 instanceof Boolean) {
                return Boolean.compare((boolean) data1, (boolean) data2);
            }
            return 0;
        };
    }

    public void setComparatorAndRefresh(String columnName) {
        if (columnName.equals(currentColumnName)) {
            comparator = Collections.reverseOrder(comparator);
        } else {
            comparator = makeComparator(columnName);
        }
        currentColumnName = columnName;
        forceRefresh(false, false, true, false);
    }

    public boolean isUsingSP() {
        return usingSP;
    }

    public void setUsingSP(boolean usingSP) {
        this.usingSP = usingSP;
    }
}
