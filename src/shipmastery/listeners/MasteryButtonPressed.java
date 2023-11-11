package shipmastery.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.UITable;
import org.jetbrains.annotations.Nullable;
import shipmastery.Settings;
import shipmastery.campaign.RefitHandler;
import shipmastery.ui.plugin.ResetButtonScript;
import shipmastery.util.ClassRefs;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MasteryButtonPressed extends ActionListener {

    ShipAPI ship;
    RefitHandler handler;
    ReflectionUtils.GenericDialogData dialogData;
    public static String tableFont = "graphics/fonts/insignia21LTaa.fnt";
    public static String orbitron24 = "graphics/fonts/orbitron24aa.fnt";
    public static float[] columnWidths = new float[] {50f, 350f, 150f, 50f, 50f, 100f, 100f};
    public static float tableEntryHeight = 38f;


    UIPanelAPI sModPanel, masteryPanel;
    ButtonAPI sModButton, masteryButton;

    public MasteryButtonPressed(RefitHandler handler) {
        this.handler = handler;
    }

    @Override
    public void trigger(Object... args) {
        dialogData = ReflectionUtils.showGenericDialog("", "Finish", 900f, 600f);

        if (dialogData == null) {
            return;
        }

        generateDialog(dialogData.dialog, dialogData.panel, false);
    }

    public void forceRefresh() {
        if (dialogData == null) return;

        generateDialog(dialogData.dialog, dialogData.panel, true);
        handler.forceRefresh(true);
    }

    public void togglePanelVisibility(ButtonAPI button) {
        if (button == sModButton) {
            ReflectionUtils.invokeMethod(sModPanel, "setOpacity", 1f);
            masteryButton.setChecked(false);
        }
        else if (button == masteryButton) {
            ReflectionUtils.invokeMethod(sModPanel, "setOpacity", 0f);
            sModButton.setChecked(false);
        }
    }

    void generateDialog(UIPanelAPI dialog, UIPanelAPI panel, boolean isRefresh) {
        ship = handler.getSelectedShip(ReflectionUtils.getCoreUI());
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

        float w = dialog.getPosition().getWidth(), h = dialog.getPosition().getHeight();
        UIPanelAPI tabButtons = makeTabButtons(120f, 40f);
        UIPanelAPI currencyPanel = makeCurrencyLabels(w);
        sModPanel = makeThisShipPanel(w, h - 100f, ship);

        panel.addComponent(tabButtons).inTMid(0f);
        panel.addComponent(sModPanel).belowMid(tabButtons, 10f);
        panel.addComponent(currencyPanel).inBMid(0f);
    }

    @SuppressWarnings("SameParameterValue")
    UIPanelAPI makeTabButtons(float w, float h) {
        float pad = 10f;

        TabButtonPressed tabButtonListener = new TabButtonPressed(this);
        CustomPanelAPI tabsPanel = Global.getSettings().createCustom(2*w + pad, h, null);
        TooltipMakerAPI thisShipTab = tabsPanel.createUIElement(w, h, false);
        thisShipTab.setAreaCheckboxFont(orbitron24);
        sModButton = thisShipTab.addAreaCheckbox("Hullmods", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w, h, 0f);
        sModButton.setChecked(true);
        ReflectionUtils.setButtonListener(sModButton, tabButtonListener);
        thisShipTab.setAreaCheckboxFontDefault();

        TooltipMakerAPI hullTypeTab = tabsPanel.createUIElement(w, h, false);
        hullTypeTab.setAreaCheckboxFont(orbitron24);
        masteryButton = hullTypeTab.addAreaCheckbox("Mastery", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), w, h, 0f);
        ReflectionUtils.setButtonListener(masteryButton, tabButtonListener);
        hullTypeTab.setAreaCheckboxFontDefault();

        tabsPanel.addUIElement(thisShipTab).inTL(-10f, 10f);
        tabsPanel.addUIElement(hullTypeTab).rightOfMid(thisShipTab, 10f);

        return tabsPanel;
    }

    UIPanelAPI makeCurrencyLabels(float width) {
        CustomPanelAPI labelsPanel = Global.getSettings().createCustom(width, 50f, null);

        int creditsAmt = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        String creditsString = "Credits: " + creditsAmt;
        float creditsStringWidth = Global.getSettings().computeStringWidth(creditsString + 10f, "graphics/fonts/orbitron20aabold.fnt");

        TooltipMakerAPI credits = labelsPanel.createUIElement(creditsStringWidth, 30f, false);
        credits.setParaOrbitronLarge();
        LabelAPI creditsLabel = credits.addPara(creditsString, 10f);
        creditsLabel.setAlignment(Alignment.LMID);
        creditsLabel.setHighlight("" + creditsAmt);
        creditsLabel.setHighlightColor(Misc.getHighlightColor());

        int masteryPointsAmt = (int) Settings.getMasteryPoints(ship.getHullSpec());
        String masteryPointsString = "Mastery Points: " + masteryPointsAmt;
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
        List<HullModSpecAPI> builtInSpecs = new ArrayList<>();
        for (String id : variant.getHullSpec().getBuiltInMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            HullModEffect effect = spec.getEffect();
            if (effect.hasSModEffect() && !effect.isSModEffectAPenalty() && !variant.getSModdedBuiltIns().contains(id)) {
                builtInSpecs.add(spec);
            }
        }

        List<HullModSpecAPI> modularSpecs = new ArrayList<>();
        for (String id : variant.getNonBuiltInHullmods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            if (!spec.isHidden() && !spec.isHiddenEverywhere()) {
                modularSpecs.add(spec);
            }
        }

        ResetButtonScript plugin = new ResetButtonScript();
        CustomPanelAPI thisShipPanel = Global.getSettings().createCustom(width, height, plugin);


        TooltipMakerAPI buildInList = thisShipPanel.createUIElement(width - 25f, height - 65f, true);

        Object[] columnData = new Object[] {"Icon", columnWidths[0], "Hullmod", columnWidths[1], "Design Type", columnWidths[2], "OP", columnWidths[3], "MP", columnWidths[4], "Credits", columnWidths[5], "Modular?", columnWidths[6]};
        UITable table = (UITable) buildInList.beginTable(
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                Misc.getBrightPlayerColor(),
                tableEntryHeight,
                true,
                true,
                columnData
        );
        ReflectionUtils.invokeMethodExtWithClasses(
                table,
                "setRowClickDelegate",
                false,
                new Class[] {ClassRefs.uiTableDelegateClass},
                new Object[] {new SModTableRowPressed(this).getProxy()});

        buildInList.addSpacer(30f);
        for (HullModSpecAPI spec : builtInSpecs) {
            addRowToHullModTable(buildInList, table, spec, false);
            buildInList.addImage(spec.getSpriteName(), 50f, tableEntryHeight - 6f, 6f);
        }
        for (HullModSpecAPI spec : modularSpecs) {
            addRowToHullModTable(buildInList, table, spec, true);
            buildInList.addImage(spec.getSpriteName(), 50f, tableEntryHeight - 6f, 6f);
        }

        buildInList.addTable("No applicable hullmods installed", -1, -buildInList.getHeightSoFar() + 10f);
        if (table.getRows().size() < 10) {
            table.autoSizeToRows(10);
        }

        float resetButtonW = 150f, resetButtonH = 30f;
        TooltipMakerAPI resetButtonTTM = thisShipPanel.createUIElement(resetButtonW, resetButtonH, false);
        resetButtonTTM.setButtonFontOrbitron20();
        ButtonAPI resetButton = resetButtonTTM.addButton("Clear S-Mods", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.TL_BR, resetButtonW, resetButtonH, 0f);
        // Whether the button is in the "confirm" state
        resetButton.setCustomData(false);
        plugin.setButton(resetButton);
        ReflectionUtils.setButtonListener(resetButton, new ClearSModsPressed(this));
        if (variant.getSMods().size() == 0) {
            resetButton.setEnabled(false);
        }

        float modularCountW = 200f, modularCountH = 40f;
        int nSMods = variant.getSMods().size();
        int sModLimit = SModUtils.getSModLimit(ship);
        TooltipMakerAPI modularCountTTM = thisShipPanel.createUIElement(modularCountW, modularCountH, false);
        modularCountTTM.setParaOrbitronVeryLarge();
        LabelAPI modularCount = modularCountTTM.addPara(String.format("Built-in: %s/%s", nSMods, sModLimit), Misc.getBrightPlayerColor(), 0f);
        modularCount.setAlignment(Alignment.LMID);
        modularCount.setHighlight("" + nSMods, "" + sModLimit);
        modularCount.setHighlightColor(Misc.getHighlightColor());

        float hintTextW = 200f, hintTextH = 40f;
        TooltipMakerAPI hintTextTTM = thisShipPanel.createUIElement(hintTextW, hintTextH, false);
        hintTextTTM.addPara("Double-click to build in", Misc.getBasePlayerColor(), 0f);

        thisShipPanel.addUIElement(buildInList).inTMid(10f);
        thisShipPanel.addUIElement(resetButtonTTM).inTR(20f,-20f);
        thisShipPanel.addUIElement(hintTextTTM).inTL(20f, -10f);
        thisShipPanel.addUIElement(modularCountTTM).inBL(20f,0f);
        return thisShipPanel;
    }

    LabelAPI label(String str, Color color) {
        LabelAPI label = Global.getSettings().createLabel(str, tableFont);
        label.setColor(color);
        return label;
    }

    void addRowToHullModTable(TooltipMakerAPI tableTTM, UITable table, final HullModSpecAPI spec, boolean modular) {
        final ShipVariantAPI variant = ship.getVariant();
        String name = Utils.shortenText(spec.getDisplayName(), tableFont, columnWidths[1]);
        String designType = spec.getManufacturer();
        Color nameColor = modular ? Misc.getBrightPlayerColor() : Color.WHITE;
        Color designColor = Misc.getGrayColor();
        String opCost = "" + spec.getCostFor(variant.getHullSize());
        int mpCost = SModUtils.getMPCost(spec, ship);
        String mpCostStr = "" + mpCost;
        int creditsCost = SModUtils.getCreditsCost(spec, ship);
        String creditsCostStr = "" + creditsCost;
        String modularString = modular ? "Yes" : "No";
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
        ReflectionUtils.invokeMethodExtWithClasses(lastRow, "setData", false, new Class[] {Object.class}, new Object[] {rowData});
    }

    /** Gives reason the mod can't be built in; returns null if hullmod can be built in */
    @Nullable String getCantBuildInReason(HullModSpecAPI spec, int mpCost, int creditsCost) {
        if (spec.hasTag(Tags.HULLMOD_NO_BUILD_IN)) return spec.getDisplayName() + " can't be built in";
        if (ship.getVariant().getSMods().size() >= SModUtils.getSModLimit(ship)) return "Build-in limit reached";

        int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        int mp = (int) Settings.getMasteryPoints(ship.getHullSpec());

        if (mpCost > mp && creditsCost > credits) return "Insufficient mastery points and credits";
        if (mpCost > mp) return "Insufficient mastery points";
        if (creditsCost > credits) return "Insufficient credits";
        return null;
     }

     public ShipAPI getShip() {
        return ship;
     }

     public static class TableRowData {
        String hullModSpecId;
        int mpCost;
        int creditsCost;
        String cantBuildInReason;

        // Can be built in <==> cantBuildInReason == null
        public TableRowData(String id, int mp, int credits, @Nullable String cantBuildInReason) {
            hullModSpecId = id;
            mpCost = mp;
            creditsCost = credits;
            this.cantBuildInReason = cantBuildInReason;
        }
     }
}
