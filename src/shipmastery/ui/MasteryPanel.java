package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.UITable;
import org.jetbrains.annotations.Nullable;
import shipmastery.Settings;
import shipmastery.campaign.RefitHandler;
import shipmastery.listeners.ClearSModsPressed;
import shipmastery.listeners.SModTableHeaderPressed;
import shipmastery.listeners.SModTableRowPressed;
import shipmastery.listeners.TabButtonPressed;
import shipmastery.ui.plugin.ButtonWithConfirmScript;
import shipmastery.util.*;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MasteryPanel {

    static final String MASTERY_TAB_STR = Utils.getString("sms_masteryPanel", "masteryTab");
    static final String CREDITS_DISPLAY_STR = Utils.getString("sms_masteryPanel", "creditsDisplay");
    static final String MASTERY_POINTS_DISPLAY_STR = Utils.getString("sms_masteryPanel", "masteryPointsDisplay");
    static final String HULLMODS_EMPTY_STR = Utils.getString("sms_masteryPanel", "hullmodListEmptyHint");
    static final String CLEAR_BUTTON_STR = Utils.getString("sms_masteryPanel", "clearButton");
    static final String BUILTIN_DISPLAY_STR = Utils.getString("sms_masteryPanel", "builtInDisplay");
    static final String DOUBLE_CLICK_HINT_STR = Utils.getString("sms_masteryPanel", "doubleClickHint");
    static final String YES_STR = Utils.getString("sms_masteryPanel", "yes");
    static final String NO_STR = Utils.getString("sms_masteryPanel", "no");
    static final String CANT_BUILD_IN_STR = Utils.getString("sms_masteryPanel", "cantBuildIn");
    static final String LIMIT_REACHED_STR = Utils.getString("sms_masteryPanel", "limitReached");
    static final String CREDITS_SHORTFALL_STR = Utils.getString("sms_masteryPanel", "notEnoughCredits");
    static final String MASTERY_POINTS_SHORTFALL_STR = Utils.getString("sms_masteryPanel", "notEnoughMasteryPoints");
    static final String DISMISS_WINDOW_STR = Utils.getString("sms_masteryPanel", "dismissWindow");
    final static String HULLMODS_TAB_STR = Utils.getString("sms_masteryPanel", "hullmodsTab");

    ShipAPI ship;
    RefitHandler handler;
    UIPanelAPI rootPanel;
    static String tableFont = Fonts.INSIGNIA_LARGE;
    static String checkboxFont = Fonts.ORBITRON_24AABOLD;
    public final static Float[] columnWidths = new Float[]{50f, 350f, 150f, 50f, 50f, 100f, 100f};
    public final static String[] columnNames = new String[]{"Icon", "Hullmod", "Design Type", "OP", "MP", "Credits", "Modular?"};
    public static float tableEntryHeight = 38f;



    String currentColumnName = columnNames[6];
    Comparator<HullModSpecAPI> comparator = makeComparator(currentColumnName);
    UIPanelAPI sModPanel, masteryPanel;
    ButtonAPI sModButton, masteryButton;

    public MasteryPanel(RefitHandler handler) {

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog("", DISMISS_WINDOW_STR, 900f, 600f);
        if (dialogData == null) return;

        rootPanel = dialogData.panel;
        this.handler = handler;
        generateDialog(rootPanel, false);
    }

    public void forceRefresh(boolean variantChanged) {
        if (rootPanel == null) return;

        if (variantChanged) {
            handler.injectRefitScreen(true);
        }

        generateDialog(rootPanel, true);
    }

    public void togglePanelVisibility(ButtonAPI button) {
        if (button == sModButton) {
            ReflectionUtils.invokeMethod(sModPanel, "setOpacity", 1f);
            ReflectionUtils.invokeMethod(masteryPanel, "setOpacity", 0f);
            masteryButton.setChecked(false);
        } else if (button == masteryButton) {
            ReflectionUtils.invokeMethod(sModPanel, "setOpacity", 0f);
            ReflectionUtils.invokeMethod(masteryPanel, "setOpacity", 1f);
            sModButton.setChecked(false);
        }
    }

    void generateDialog(UIPanelAPI panel, boolean isRefresh) {
        ship = handler.getSelectedShip();
        if (ship == null) {
            return;
        }

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
        sModPanel = makeThisShipPanel(w, h - 100f, ship);
        masteryPanel = makeMasteryPanel(w, h - 100f, ship);
        togglePanelVisibility(sModButton);

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
        sModButton = thisShipTab.addAreaCheckbox(HULLMODS_TAB_STR, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w, h, 0f);
        sModButton.setChecked(true);
        ReflectionUtils.setButtonListener(sModButton, tabButtonListener);
        thisShipTab.setAreaCheckboxFontDefault();

        TooltipMakerAPI hullTypeTab = tabsPanel.createUIElement(w, h, false);
        hullTypeTab.setAreaCheckboxFont(checkboxFont);
        masteryButton = hullTypeTab.addAreaCheckbox(MASTERY_TAB_STR, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w, h, 0f);
        ReflectionUtils.setButtonListener(masteryButton, tabButtonListener);
        hullTypeTab.setAreaCheckboxFontDefault();

        tabsPanel.addUIElement(thisShipTab).inTL(-10f, 10f);
        tabsPanel.addUIElement(hullTypeTab).rightOfMid(thisShipTab, 10f);

        return tabsPanel;
    }

    UIPanelAPI makeCurrencyLabels(float width) {
        CustomPanelAPI labelsPanel = Global.getSettings().createCustom(width, 50f, null);

        int creditsAmt = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        String creditsAmtFmt = Misc.getFormat().format(creditsAmt);
        String creditsString = CREDITS_DISPLAY_STR + creditsAmtFmt;
        float creditsStringWidth = Global.getSettings().computeStringWidth(creditsString + 10f, "graphics/fonts/orbitron20aabold.fnt");

        TooltipMakerAPI credits = labelsPanel.createUIElement(creditsStringWidth, 30f, false);
        credits.setParaOrbitronLarge();
        LabelAPI creditsLabel = credits.addPara(creditsString, 10f);
        creditsLabel.setAlignment(Alignment.LMID);
        creditsLabel.setHighlight("" + creditsAmtFmt);
        creditsLabel.setHighlightColor(Misc.getHighlightColor());

        int masteryPointsAmt = (int) Settings.getMasteryPoints(ship.getHullSpec());
        String masteryPointsString = MASTERY_POINTS_DISPLAY_STR + masteryPointsAmt;
        float masteryPointsStringWidth = Global.getSettings().computeStringWidth(masteryPointsString + 10f, "graphics/fonts/orbitron20aabold.fnt");

        TooltipMakerAPI masteryPoints = labelsPanel.createUIElement(masteryPointsStringWidth, 30f, false);
        masteryPoints.setParaOrbitronLarge();
        LabelAPI masteryPointsLabel = masteryPoints.addPara(masteryPointsString, 10f);
        masteryPointsLabel.setAlignment(Alignment.LMID);
        masteryPointsLabel.setHighlight("" + masteryPointsAmt);
        masteryPointsLabel.setHighlightColor(Settings.masteryColor);

        labelsPanel.addUIElement(credits).inBL(20f, 10f);
        labelsPanel.addUIElement(masteryPoints).inBMid(10f);
        return labelsPanel;
    }

    UIPanelAPI makeThisShipPanel(float width, float height, ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();

        List<HullModSpecAPI> applicableSpecs = new ArrayList<>();
        for (String id : variant.getHullSpec().getBuiltInMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            HullModEffect effect = spec.getEffect();
            if (effect.hasSModEffect() && !effect.isSModEffectAPenalty() && !variant.getSModdedBuiltIns().contains(id)) {
                applicableSpecs.add(spec);
            }
        }
        for (String id : variant.getNonBuiltInHullmods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (!spec.isHidden() && !spec.isHiddenEverywhere()) {
                applicableSpecs.add(spec);
            }
        }
        Collections.sort(applicableSpecs, comparator);

        ButtonWithConfirmScript plugin = new ButtonWithConfirmScript();
        CustomPanelAPI thisShipPanel = Global.getSettings().createCustom(width, height, plugin);

        Object[] columnData = Utils.interleaveArrays(columnNames, columnWidths);

        TooltipMakerAPI buildInListHeader = thisShipPanel.createUIElement(width - 25f, 25f, false);
        UITable headerTable = (UITable) buildInListHeader.beginTable(Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(),
                tableEntryHeight,
                false,
                true,
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
        UITable table = (UITable) buildInList.beginTable(
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(),
                tableEntryHeight,
                true,
                false,
                columnData
        );
        ReflectionUtils.invokeMethodExtWithClasses(
                table,
                "setRowClickDelegate",
                false,
                new Class[]{ClassRefs.uiTableDelegateClass},
                new Object[]{new SModTableRowPressed(this).getProxy()});

        buildInList.addSpacer(7f);

        for (HullModSpecAPI spec : applicableSpecs) {
            addRowToHullModTable(buildInList, table, spec, !SModUtils.isHullmodBuiltIn(spec, ship.getVariant()));
            buildInList.addImage(spec.getSpriteName(), 50f, tableEntryHeight - 6f, 6f);
        }

        buildInList.addTable(HULLMODS_EMPTY_STR, -1, -buildInList.getHeightSoFar() + 10f);
        if (table.getRows().size() < 10) {
            table.autoSizeToRows(10);
        }

        float resetButtonW = 150f, resetButtonH = 30f;
        TooltipMakerAPI resetButtonTTM = thisShipPanel.createUIElement(resetButtonW, resetButtonH, false);
        resetButtonTTM.setButtonFontOrbitron20();
        ButtonAPI resetButton = resetButtonTTM.addButton(CLEAR_BUTTON_STR, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, resetButtonW, resetButtonH, 0f);
        // Whether the button is in the "confirm" state
        resetButton.setCustomData(false);
        plugin.setButton(resetButton);
        ReflectionUtils.setButtonListener(resetButton, new ClearSModsPressed(this));
        if (variant.getSMods().size() == 0) {
            resetButton.setEnabled(false);
        }

        float modularCountW = 200f, modularCountH = 40f;
        int nSMods = variant.getSMods().size();
        int sModLimit = Misc.getMaxPermanentMods(ship);
        TooltipMakerAPI modularCountTTM = thisShipPanel.createUIElement(modularCountW, modularCountH, false);
        modularCountTTM.setParaOrbitronVeryLarge();
        LabelAPI modularCount = modularCountTTM.addPara(BUILTIN_DISPLAY_STR + String.format("%s/%s", nSMods, sModLimit), Misc.getBrightPlayerColor(), 0f);
        modularCount.setAlignment(Alignment.LMID);
        modularCount.setHighlight("" + nSMods, "" + sModLimit);
        modularCount.setHighlightColor(Misc.getHighlightColor());

        float hintTextW = 200f, hintTextH = 40f;
        TooltipMakerAPI hintTextTTM = thisShipPanel.createUIElement(hintTextW, hintTextH, false);
        hintTextTTM.addPara(DOUBLE_CLICK_HINT_STR, Misc.getBasePlayerColor(), 0f);

        thisShipPanel.addUIElement(buildInList).inTMid(37f);
        thisShipPanel.addUIElement(buildInListHeader).inTMid(20f);
        thisShipPanel.addUIElement(resetButtonTTM).inTR(30f, -20f);
        thisShipPanel.addUIElement(hintTextTTM).inTL(20f, -5f);
        thisShipPanel.addUIElement(modularCountTTM).inBR(-15f, -10f);
        return thisShipPanel;
    }

    UIPanelAPI makeMasteryPanel(float width, float height, ShipAPI ship) {
        CustomPanelAPI masteryPanel = Global.getSettings().createCustom(width, height, null);

        float shipDisplaySize = 250;
        TooltipMakerAPI shipDisplay = masteryPanel.createUIElement(shipDisplaySize, shipDisplaySize + 25f, false);
        ShipHullSpecAPI hullSpec = ship.getHullSpec();
        ShipHullSpecAPI baseHullSpec = Global.getSettings().getHullSpec(Utils.getBaseHullId(hullSpec));
        String spriteName = baseHullSpec.getSpriteName();
        SpriteAPI sprite = Global.getSettings().getSprite(spriteName);
        float size = Math.max(sprite.getWidth(), sprite.getHeight());
        size = Math.min(size, shipDisplaySize - 10f);
        shipDisplay.setParaOrbitronLarge();
        shipDisplay.addPara(baseHullSpec.getNameWithDesignationWithDashClass(), 0f).setAlignment(Alignment.MID);
        ButtonAPI outline = shipDisplay.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), shipDisplaySize-5f, shipDisplaySize-5f, 0f);
        outline.setClickable(false);
        outline.setGlowBrightness(0f);
        outline.setMouseOverSound(null);
        shipDisplay.addImage(baseHullSpec.getSpriteName(), shipDisplaySize, size, -size - (shipDisplaySize - size) / 2f);
        masteryPanel.addUIElement(shipDisplay).inLMid(30f);

        float containerW = 500f, containerH = 450f;
        TooltipMakerAPI masteryContainer = masteryPanel.createUIElement(containerW, containerH, false);
        ButtonAPI containerOutline = masteryContainer.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), containerW-5f, containerH-5f, 0f);
        containerOutline.setClickable(false);
        containerOutline.setGlowBrightness(0f);
        containerOutline.setMouseOverSound(null);
        masteryPanel.addUIElement(masteryContainer).inRMid(50f);

        float containerPadX = 4f, containerPadY = 8f;
        TooltipMakerAPI masteryDisplay = masteryPanel.createUIElement(containerW + 25f - containerPadX, containerH - containerPadY, true);
        masteryDisplay.setParaInsigniaLarge();
        for (int i = 0; i < MasteryUtils.getMaxMastery(hullSpec); i++) {
            LabelAPI label = masteryDisplay.addPara(MasteryUtils.getMasteryDescription(hullSpec, i), 0f);
            label.setAlignment(Alignment.LMID);
            label.getPosition().setXAlignOffset(25f);
            float labelHeight = label.getPosition().getHeight();
            masteryDisplay.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), containerW - containerPadX, labelHeight, -labelHeight);
            masteryDisplay.addPara(MasteryUtils.getMasteryDescription(hullSpec, i), -labelHeight).setAlignment(Alignment.LMID);
            ButtonAPI levelIndicator = masteryDisplay.addAreaCheckbox("" + i, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), 25f, labelHeight, -labelHeight);
            levelIndicator.getPosition().setXAlignOffset(-25f);
        }
        masteryPanel.addUIElement(masteryDisplay).inRMid(50f).setYAlignOffset(5f);

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
        final ShipVariantAPI variant = ship.getVariant();
        String name = Utils.shortenText(spec.getDisplayName(), tableFont, columnWidths[1]);
        String designType = Utils.shortenText(spec.getManufacturer(), Fonts.DEFAULT_SMALL, columnWidths[2]);
        Color nameColor = modular ? Misc.getBrightPlayerColor() : Color.WHITE;
        Color designColor = Misc.getGrayColor();
        String opCost = "" + spec.getCostFor(variant.getHullSize());
        int mpCost = SModUtils.getMPCost(spec, ship);
        String mpCostStr = "" + mpCost;
        int creditsCost = SModUtils.getCreditsCost(spec, ship);
        String creditsCostStr = Misc.getFormat().format(creditsCost);
        String modularString = modular ? YES_STR : NO_STR;
        Color masteryColor = Settings.masteryColor;
        Color creditsColor = Misc.getHighlightColor();
        String cantBuildInReason = getCantBuildInReason(spec, mpCost, creditsCost);

        if (cantBuildInReason != null) {
            nameColor = masteryColor = creditsColor = Misc.getGrayColor();
        }

        tableTTM.addRowWithGlow(
                Alignment.MID, nameColor, " ",
                Alignment.LMID, nameColor, label(name, nameColor),
                Alignment.MID, designColor, designType,
                Alignment.MID, designColor, opCost,
                Alignment.MID, Settings.masteryColor, label(mpCostStr, masteryColor),
                Alignment.MID, Misc.getHighlightColor(), label(creditsCostStr, creditsColor),
                Alignment.MID, nameColor, label(modularString, nameColor)
        );

        tableTTM.addTooltipToAddedRow(
                new TooltipMakerAPI.TooltipCreator() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        HullModEffect effect = spec.getEffect();
                        ShipAPI.HullSize hullSize = variant.getHullSize();
                        tooltip.addTitle(spec.getDisplayName());
                        tooltip.addSpacer(10f);
                        if (effect.shouldAddDescriptionToTooltip(hullSize, null, true)) {
                            List<String> highlights = new ArrayList<>();
                            String descParam;
                            // hard cap at 100 just in case getDescriptionParam for some reason
                            // doesn't default to null
                            for (int i = 0; i < 100 && (descParam = effect.getDescriptionParam(i, hullSize, null)) != null; i++) {
                                highlights.add(descParam);
                            }
                            tooltip.addPara(spec.getDescription(hullSize).replaceAll("%", "%%"), 0f, Misc.getHighlightColor(), highlights.toArray(new String[0]));
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
                }
                , TooltipMakerAPI.TooltipLocation.RIGHT, false);

        TableRowData rowData = new TableRowData(spec.getId(), mpCost, creditsCost, cantBuildInReason);
        List<?> rows = (List<?>) ReflectionUtils.invokeMethod(table, "getRows");
        Object lastRow = rows.get(rows.size() - 1);
        ReflectionUtils.invokeMethodExtWithClasses(lastRow, "setData", false, new Class[]{Object.class}, new Object[]{rowData});
    }

    /**
     * Gives reason the mod can't be built in; returns null if hullmod can be built in
     */
    @Nullable String getCantBuildInReason(HullModSpecAPI spec, int mpCost, int creditsCost) {
        if (spec.hasTag(Tags.HULLMOD_NO_BUILD_IN)) return spec.getDisplayName() + CANT_BUILD_IN_STR;
        if (ship.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(ship)) return LIMIT_REACHED_STR;

        int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        int mp = (int) Settings.getMasteryPoints(ship.getHullSpec());

        String notEnoughCredits = CREDITS_SHORTFALL_STR;
        String notEnoughMasteryPoints = MASTERY_POINTS_SHORTFALL_STR;
        if (mpCost > mp && creditsCost > credits) return notEnoughMasteryPoints + ", " + notEnoughCredits;
        if (mpCost > mp) return notEnoughMasteryPoints;
        if (creditsCost > credits) return notEnoughCredits;
        return null;
    }

    public ShipAPI getShip() {
        return ship;
    }

    public static class TableRowData {
        public String hullModSpecId;
        public int mpCost;
        public int creditsCost;
        public String cantBuildInReason;

        // Can be built in <==> cantBuildInReason == null
        public TableRowData(String id, int mp, int credits, @Nullable String cantBuildInReason) {
            hullModSpecId = id;
            mpCost = mp;
            creditsCost = credits;
            this.cantBuildInReason = cantBuildInReason;
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
            return spec.getCostFor(ship.getHullSize());
        }

        if (columnNames[4].equals(columnName)) {
            return SModUtils.getMPCost(spec, ship);
        }

        if (columnNames[5].equals(columnName)) {
            return SModUtils.getCreditsCost(spec, ship);
        }

        if (columnNames[6].equals(columnName)) {
            return !SModUtils.isHullmodBuiltIn(spec, ship.getVariant());
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
        }
        else {
            comparator = makeComparator(columnName);
        }
        currentColumnName = columnName;
        forceRefresh(false);
    }
}
