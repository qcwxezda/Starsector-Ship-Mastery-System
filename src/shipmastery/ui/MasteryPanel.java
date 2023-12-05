package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.ui.UITable;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.campaign.RefitHandler;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.deferred.Action;
import shipmastery.ui.triggers.*;
import shipmastery.util.*;

import java.awt.Color;
import java.util.*;

public class MasteryPanel {
    ShipAPI root;
    ShipAPI module;
    RefitHandler handler;
    UIPanelAPI rootPanel;
    static String tableFont = Fonts.INSIGNIA_LARGE;
    static String checkboxFont = Fonts.ORBITRON_24AABOLD;
    public final static Float[] columnWidths = new Float[]{50f, 350f, 150f, 75f, 75f, 150f, 100f};
    public final static String[] columnNames =
            new String[]{
                    Strings.ICON_HEADER,
                    Strings.HULLMOD_HEADER,
                    Strings.DESIGN_TYPE_HEADER,
                    Strings.ORDNANCE_POINTS_HEADER,
                    Strings.MASTERY_POINTS_HEADER,
                    Strings.CREDITS_HEADER,
                    Strings.MODULAR_HEADER};
    public static float tableEntryHeight = 38f;


    String currentColumnName = columnNames[6];
    Comparator<HullModSpecAPI> comparator = makeComparator(currentColumnName);
    UIPanelAPI sModPanel, masteryPanel;
    ButtonAPI sModButton, masteryButton;
    boolean isShowingMasteryPanel = false;
    boolean isInRestorableMarket = false;
    MasteryDisplay savedMasteryDisplay;
    TooltipMakerAPI upgradeMasteryDisplay, confirmOrCancelDisplay;
    int currentMastery, maxMastery;

    public MasteryPanel(RefitHandler handler) {

        ReflectionUtils.GenericDialogData dialogData =
                ReflectionUtils.showGenericDialog("", Strings.DISMISS_WINDOW_STR, 1000f, 600f);
        if (dialogData == null) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.CANT_OPEN_PANEL, Settings.NEGATIVE_HIGHLIGHT_COLOR);
            return;
        }


        rootPanel = dialogData.panel;
        this.handler = handler;
        generateDialog(rootPanel, false, false);
    }

    public void forceRefresh(boolean shouldSync, boolean shouldSaveIfSynced, boolean useSavedScrollerLocation) {
        if (rootPanel == null) return;

        handler.injectRefitScreen(shouldSync, shouldSaveIfSynced);
        if (useSavedScrollerLocation) {
            savedMasteryDisplay.saveScrollerHeight();
        }

        generateDialog(rootPanel, true, useSavedScrollerLocation);
    }

    public void togglePanelVisibility(ButtonAPI button) {
        if (button == sModButton) {
            ReflectionUtils.invokeMethod(sModPanel, "setOpacity", 1f);
            ReflectionUtils.invokeMethod(masteryPanel, "setOpacity", 0f);
            isShowingMasteryPanel = false;
            masteryButton.setChecked(false);
            sModButton.setChecked(true);
        } else if (button == masteryButton) {
            ReflectionUtils.invokeMethod(sModPanel, "setOpacity", 0f);
            ReflectionUtils.invokeMethod(masteryPanel, "setOpacity", 1f);
            isShowingMasteryPanel = true;
            sModButton.setChecked(false);
            masteryButton.setChecked(true);
        }
    }

    void generateDialog(UIPanelAPI panel, boolean isRefresh, boolean useSavedScrollerLocation) {
        Pair<ShipAPI, ShipAPI> moduleAndShip = handler.getSelectedShip();
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
            Iterator<?> itr = children.listIterator();
            while (itr.hasNext()) {
                Object o = itr.next();
                if (o instanceof UIPanelAPI) {
                    itr.remove();
                }
            }
        }

        float w = panel.getPosition().getWidth() + 20f, h = panel.getPosition().getHeight();
        UIPanelAPI tabButtons = makeTabButtons(120f, 40f);
        UIPanelAPI currencyPanel = makeCurrencyLabels(w);
        sModPanel = makeThisShipPanel(w, h - 100f);
        masteryPanel = makeMasteryPanel(w, h - 100f, useSavedScrollerLocation);
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
        sModButton = thisShipTab.addAreaCheckbox(Strings.HULLMODS_TAB_STR, null, Misc.getBasePlayerColor(),
                                                 Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w, h, 0f);
        sModButton.setShortcut(Keyboard.KEY_1, false);
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
                    tooltip.addPara(Strings.MUST_BE_DOCKED_HULLMODS, 0f);
                }
            }, TooltipMakerAPI.TooltipLocation.BELOW, false);
        }

        TooltipMakerAPI hullTypeTab = tabsPanel.createUIElement(w, h, false);
        hullTypeTab.setAreaCheckboxFont(checkboxFont);
        masteryButton =
                hullTypeTab.addAreaCheckbox(Strings.MASTERY_TAB_STR, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                            Misc.getBrightPlayerColor(), w, h, 0f);
        masteryButton.setShortcut(Keyboard.KEY_2, false);
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
        String creditsString = Strings.CREDITS_DISPLAY_STR + creditsAmtFmt;
        float creditsStringWidth =
                Global.getSettings().computeStringWidth(creditsString + 10f, "graphics/fonts/orbitron20aabold.fnt");

        TooltipMakerAPI credits = labelsPanel.createUIElement(creditsStringWidth, 30f, false);
        credits.setParaOrbitronLarge();
        LabelAPI creditsLabel = credits.addPara(creditsString, 10f);
        creditsLabel.setAlignment(Alignment.LMID);
        creditsLabel.setHighlight("" + creditsAmtFmt);
        creditsLabel.setHighlightColor(Misc.getHighlightColor());

        int masteryPointsAmt = (int) ShipMastery.getPlayerMasteryPoints(root.getHullSpec());
        String masteryPointsString = Strings.MASTERY_POINTS_DISPLAY_STR + masteryPointsAmt;
        float masteryPointsStringWidth = Global.getSettings().computeStringWidth(masteryPointsString + 10f,
                                                                                 "graphics/fonts/orbitron20aabold.fnt");

        TooltipMakerAPI masteryPoints = labelsPanel.createUIElement(masteryPointsStringWidth, 30f, false);
        masteryPoints.setParaOrbitronLarge();
        LabelAPI masteryPointsLabel = masteryPoints.addPara(masteryPointsString, 10f);
        masteryPointsLabel.setAlignment(Alignment.LMID);
        masteryPointsLabel.setHighlight("" + masteryPointsAmt);
        masteryPointsLabel.setHighlightColor(Settings.MASTERY_COLOR);

        labelsPanel.addUIElement(credits).inBL(20f, 10f);
        labelsPanel.addUIElement(masteryPoints).inBMid(10f);
        return labelsPanel;
    }

    UIPanelAPI makeThisShipPanel(float width, float height) {
        ShipVariantAPI moduleVariant = module.getVariant();
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
        Collections.sort(applicableSpecs, comparator);

        CustomPanelAPI thisShipPanel = Global.getSettings().createCustom(width, height, null);
        Object[] columnData = Utils.interleaveArrays(columnNames, columnWidths);

        TooltipMakerAPI buildInListHeader = thisShipPanel.createUIElement(width - 25f, 25f, false);
        UITable headerTable =
                (UITable) buildInListHeader.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                       Misc.getBrightPlayerColor(), tableEntryHeight, false, true,
                                                       columnData);
        Object header = ReflectionUtils.invokeMethod(headerTable, "getHeader");
        List<?> headerChildren = (List<?>) ReflectionUtils.invokeMethod(header, "getChildrenNonCopy");
        for (int i = 0; i < headerChildren.size(); i++) {
            Object child = headerChildren.get(i);

            if (!(child instanceof ButtonAPI)) continue;
            ButtonAPI headerButton = (ButtonAPI) child;
            if (columnNames[i].equals(currentColumnName)) {
                headerButton.highlight();
            }
            ReflectionUtils.setButtonListener(headerButton, new SModTableHeaderPressed(this, i));
        }
        buildInListHeader.addTable("", -1, 0f);

        TooltipMakerAPI buildInList = thisShipPanel.createUIElement(width - 25f, height - 75f, true);
        UITable table = (UITable) buildInList.beginTable(Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                         Misc.getBrightPlayerColor(), tableEntryHeight, true, false,
                                                         columnData);
        ReflectionUtils.invokeMethodExtWithClasses(table, "setRowClickDelegate", false,
                                                   new Class[]{ClassRefs.uiTableDelegateClass},
                                                   new SModTableRowPressed(this, module).getProxy());

        buildInList.addSpacer(7f);

        for (HullModSpecAPI spec : applicableSpecs) {
            addRowToHullModTable(buildInList, table, spec, !SModUtils.isHullmodBuiltIn(spec, moduleVariant));
            buildInList.addImage(spec.getSpriteName(), 50f, tableEntryHeight - 6f, 6f);
        }

        buildInList.addTable(Strings.HULLMODS_EMPTY_STR, -1, -buildInList.getHeightSoFar() + 10f);
        if (table.getRows().size() < 10) {
            table.autoSizeToRows(10);
        }

        float resetButtonW = 150f, resetButtonH = 30f;
        TooltipMakerAPI resetButtonTTM = thisShipPanel.createUIElement(resetButtonW, resetButtonH, false);
        resetButtonTTM.setButtonFontOrbitron20();
        ButtonAPI resetButton =
                resetButtonTTM.addButton(Strings.CLEAR_BUTTON_STR, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                         Alignment.MID, CutStyle.TL_BR, resetButtonW, resetButtonH, 0f);
        ReflectionUtils.setButtonListener(resetButton, new ClearSModsPressed(this, module, Strings.CLEAR_BUTTON_STR));
        if (moduleVariant.getSMods().size() == 0) {
            resetButton.setEnabled(false);
        }

        if (!TransientSettings.SMOD_REMOVAL_ENABLED) {
            ReflectionUtils.invokeMethod(resetButton, "setOpacity", 0f);
        }

        float modularCountW = 200f, modularCountH = 40f;
        int nSMods = moduleVariant.getSMods().size();
        int sModLimit = Misc.getMaxPermanentMods(module);
        TooltipMakerAPI modularCountTTM = thisShipPanel.createUIElement(modularCountW, modularCountH, false);
        modularCountTTM.setParaOrbitronVeryLarge();
        LabelAPI modularCount = modularCountTTM.addPara(
                Strings.BUILTIN_DISPLAY_STR + String.format("%s/%s", nSMods, sModLimit),
                Misc.getBrightPlayerColor(), 0f);
        modularCount.setAlignment(Alignment.RMID);
        modularCount.setHighlight("" + nSMods, "" + sModLimit);
        modularCount.setHighlightColor(Misc.getHighlightColor());

        float hintTextW = 200f, hintTextH = 40f;
        TooltipMakerAPI hintTextTTM = thisShipPanel.createUIElement(hintTextW, hintTextH, false);
        hintTextTTM.addPara(Strings.DOUBLE_CLICK_HINT_STR, Misc.getBasePlayerColor(), 0f);

        thisShipPanel.addUIElement(buildInList).inTMid(37f);
        thisShipPanel.addUIElement(buildInListHeader).inTMid(20f);

        thisShipPanel.addUIElement(resetButtonTTM).inTR(30f, -20f);

        thisShipPanel.addUIElement(hintTextTTM).inTL(20f, -5f);
        thisShipPanel.addUIElement(modularCountTTM).inBR(20f, -10f);
        return thisShipPanel;
    }

    void showUpgradeOrConfirmation() {
        if (Objects.equals(savedMasteryDisplay.getActiveLevels(), savedMasteryDisplay.getSelectedLevels())) {
            ReflectionUtils.invokeMethod(upgradeMasteryDisplay, "setOpacity", currentMastery >= maxMastery ? 0f : 1f);
            ReflectionUtils.invokeMethod(confirmOrCancelDisplay, "setOpacity", 0f);
        }
        else {
            ReflectionUtils.invokeMethod(upgradeMasteryDisplay, "setOpacity", 0f);
            ReflectionUtils.invokeMethod(confirmOrCancelDisplay, "setOpacity", 1f);
        }
    }

    public Map<Integer, Boolean> getSelectedMasteryButtons() {
        return savedMasteryDisplay == null ? new HashMap<Integer, Boolean>() : savedMasteryDisplay.getSelectedLevels();
    }

    UIPanelAPI makeMasteryPanel(float width, float height, boolean useSavedScrollerLocation) {
        final ShipHullSpecAPI baseHullSpec = Utils.getRestoredHullSpec(root.getHullSpec());
        currentMastery = ShipMastery.getPlayerMasteryLevel(baseHullSpec);
        maxMastery = ShipMastery.getMaxMasteryLevel(baseHullSpec);

        CustomPanelAPI masteryPanel = Global.getSettings().createCustom(width, height, null);
        float shipDisplaySize = 250f;
        TooltipMakerAPI shipDisplay = masteryPanel.createUIElement(shipDisplaySize, shipDisplaySize + 25f, false);
        new ShipDisplay(baseHullSpec, shipDisplaySize).create(shipDisplay);
        masteryPanel.addUIElement(shipDisplay).inTL(50f, 70f);

        upgradeMasteryDisplay = masteryPanel.createUIElement(200f, 100f, false);
        new UpgradeMasteryDisplay(this, baseHullSpec).create(upgradeMasteryDisplay);
        masteryPanel.addUIElement(upgradeMasteryDisplay).belowMid(shipDisplay, 10f);

        if (currentMastery >= maxMastery) {
            ReflectionUtils.invokeMethod(upgradeMasteryDisplay, "setOpacity", 0f);
        }

        confirmOrCancelDisplay = masteryPanel.createUIElement(225f, 100f, false);
        new ConfirmOrCancelDisplay(new ConfirmMasteryChangesPressed(this, root.getHullSpec()), new CancelMasteryChangesPressed(this)).create(confirmOrCancelDisplay);
        masteryPanel.addUIElement(confirmOrCancelDisplay).belowMid(shipDisplay, 10f);

        ReflectionUtils.invokeMethod(confirmOrCancelDisplay, "setOpacity", 0f);

        float containerW = 600f, containerH = 450f;
        TooltipMakerAPI masteryContainer = masteryPanel.createUIElement(containerW, containerH, false);
        new MasteryDisplayOutline(containerW, containerH).create(masteryContainer);
        masteryPanel.addUIElement(masteryContainer).inRMid(50f);

        float containerPadX = 4f, containerPadY = 8f;
        TooltipMakerAPI masteryDisplayTTM =
                masteryPanel.createUIElement(containerW + 50f - containerPadX, containerH + 2f - containerPadY, true);

        float pad = 25f;
        MasteryDisplay display = new MasteryDisplay(module, root, containerW, containerH, pad, !useSavedScrollerLocation, new Action() {
            @Override
            public void perform() {
                showUpgradeOrConfirmation();
            }
        });
        display.create(masteryDisplayTTM);
        masteryDisplayTTM.setHeightSoFar(display.getTotalHeight() - pad);
        masteryPanel.addUIElement(masteryDisplayTTM).inTR(50f, 18f);

        if (savedMasteryDisplay != null && useSavedScrollerLocation) {
            display.scrollToHeight(savedMasteryDisplay.getSavedScrollerHeight());
        }
        else {
            display.scrollToHeight(Math.max(0f, Math.min(display.getScrollToHeight() - pad, display.getTotalHeight() - containerH - pad)));
        }

        savedMasteryDisplay = display;
        return masteryPanel;
    }

    LabelAPI label(String str, Color color) {
        return label(str, color, tableFont);
    }

    LabelAPI label(String str, Color color, String font) {
        LabelAPI label = Global.getSettings().createLabel(str, font);
        label.setColor(color);
        return label;
    }

    void addRowToHullModTable(TooltipMakerAPI tableTTM, UITable table, final HullModSpecAPI spec, boolean modular) {
        String name = Utils.shortenText(spec.getDisplayName(), tableFont, columnWidths[1]);
        String designType = Utils.shortenText(spec.getManufacturer(), Fonts.DEFAULT_SMALL, columnWidths[2]);
        Color nameColor = modular ? Misc.getBrightPlayerColor() : Color.WHITE;
        Color designColor = Misc.getGrayColor();
        String opCost = "" + (modular ? spec.getCostFor(module.getHullSize()) : 0);
        int mpCost = SModUtils.getMPCost(spec, module);
        String mpCostStr = "" + mpCost;
        int creditsCost = SModUtils.getCreditsCost(spec, module);
        String creditsCostStr = Misc.getFormat().format(creditsCost);
        String modularString = modular ? Strings.YES_STR : Strings.NO_STR;
        Color masteryColor = Settings.MASTERY_COLOR;
        Color creditsColor = Misc.getHighlightColor();
        String cantBuildInReason = getCantBuildInReason(spec, modular, mpCost, creditsCost);

        if (cantBuildInReason != null) {
            nameColor = masteryColor = creditsColor = Misc.getGrayColor();
        }

        tableTTM.addRowWithGlow(Alignment.MID, nameColor, " ", Alignment.LMID, nameColor, label(name, nameColor),
                                Alignment.MID, designColor, designType, Alignment.MID, designColor, opCost,
                                Alignment.MID, Settings.MASTERY_COLOR, label(mpCostStr, masteryColor), Alignment.MID,
                                Misc.getHighlightColor(), label(creditsCostStr, creditsColor), Alignment.MID, nameColor,
                                label(modularString, nameColor));

        tableTTM.addTooltipToAddedRow(new TooltipMakerAPI.TooltipCreator() {
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                HullModEffect effect = spec.getEffect();
                ShipAPI.HullSize hullSize = module.getHullSize();
                tooltip.addTitle(spec.getDisplayName());
                tooltip.addSpacer(10f);
                if (effect.shouldAddDescriptionToTooltip(hullSize, null, true)) {
                    List<String> highlights = new ArrayList<>();
                    String descParam;
                    // hard cap at 100 just in case getDescriptionParam for some reason
                    // doesn't default to null
                    for (int i = 0; i < 100 && (descParam = effect.getDescriptionParam(i, hullSize, null)) != null;
                         i++) {
                        highlights.add(descParam);
                    }
                    tooltip.addPara(spec.getDescription(hullSize).replaceAll("%", "%%"), 0f, Misc.getHighlightColor(),
                                    highlights.toArray(new String[0]));
                }
                effect.addPostDescriptionSection(tooltip, hullSize, null, getTooltipWidth(tooltipParam), true);
                if (effect.hasSModEffectSection(hullSize, null, false)) {
                    effect.addSModSection(tooltip, hullSize, null, getTooltipWidth(tooltipParam), true, true);
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

        TableRowData rowData = new TableRowData(spec.getId(), mpCost, creditsCost, cantBuildInReason, modular);
        List<?> rows = (List<?>) ReflectionUtils.invokeMethod(table, "getRows");
        Object lastRow = rows.get(rows.size() - 1);
        ReflectionUtils.invokeMethodExtWithClasses(
                lastRow, "setData", false, new Class[]{Object.class}, rowData);
    }

    /**
     * Gives reason the mod can't be built in; returns null if hullmod can be built in
     */
    @Nullable String getCantBuildInReason(HullModSpecAPI spec, boolean modular, int mpCost, int creditsCost) {
        if (spec.hasTag(Tags.HULLMOD_NO_BUILD_IN) && !TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.contains(spec.getId())) {
            return spec.getDisplayName() + Strings.CANT_BUILD_IN_STR;
        }
        if (module.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(module)
                + TransientSettings.OVER_LIMIT_SMOD_COUNT.getModifiedInt() && modular) {
            return Strings.LIMIT_REACHED_STR;
        }

        int credits = (int) Utils.getPlayerCredits().get();
        int mp = (int) ShipMastery.getPlayerMasteryPoints(root.getHullSpec());

        String notEnoughCredits = Strings.CREDITS_SHORTFALL_STR;
        String notEnoughMasteryPoints = Strings.MASTERY_POINTS_SHORTFALL_STR;
        if (mpCost > mp && creditsCost > credits) return notEnoughMasteryPoints + ", " + notEnoughCredits;
        if (mpCost > mp) return notEnoughMasteryPoints;
        if (creditsCost > credits) return notEnoughCredits;
        return null;
    }

    public static class TableRowData {
        public String hullModSpecId;
        public int mpCost;
        public int creditsCost;
        public String cantBuildInReason;
        public boolean isModular;

        // Can be built in <==> cantBuildInReason == null
        public TableRowData(String id, int mp, int credits, @Nullable String cantBuildInReason, boolean isModular) {
            hullModSpecId = id;
            mpCost = mp;
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
            return SModUtils.getMPCost(spec, module);
        }

        if (columnNames[5].equals(columnName)) {
            return SModUtils.getCreditsCost(spec, module);
        }

        if (columnNames[6].equals(columnName)) {
            return !SModUtils.isHullmodBuiltIn(spec, module.getVariant());
        }

        return null;
    }

    Comparator<HullModSpecAPI> makeComparator(final String columnName) {
        return new Comparator<HullModSpecAPI>() {
            @Override
            public int compare(HullModSpecAPI spec1, HullModSpecAPI spec2) {
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
            }
        };
    }

    public void setComparatorAndRefresh(String columnName) {
        if (columnName.equals(currentColumnName)) {
            comparator = Collections.reverseOrder(comparator);
        } else {
            comparator = makeComparator(columnName);
        }
        currentColumnName = columnName;
        forceRefresh(false, false, true);
    }
}
