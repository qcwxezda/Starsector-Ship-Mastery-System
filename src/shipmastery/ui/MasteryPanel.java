package shipmastery.ui;

import com.fs.graphics.util.Fader;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.ui.UITable;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.campaign.RefitHandler;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.ui.triggers.*;
import shipmastery.util.*;

import java.awt.*;
import java.util.List;
import java.util.*;

public class MasteryPanel {
    ShipAPI ship;
    RefitHandler handler;
    UIPanelAPI rootPanel;
    static String tableFont = Fonts.INSIGNIA_LARGE;
    static String checkboxFont = Fonts.ORBITRON_24AABOLD;
    public final static Float[] columnWidths = new Float[]{50f, 350f, 150f, 50f, 50f, 100f, 100f};
    public final static String[] columnNames =
            new String[]{"Icon", "Hullmod", "Design Type", "OP", "MP", "Credits", "Modular?"};
    public static float tableEntryHeight = 38f;


    String currentColumnName = columnNames[6];
    Comparator<HullModSpecAPI> comparator = makeComparator(currentColumnName);
    UIPanelAPI sModPanel, masteryPanel;
    ButtonAPI sModButton, masteryButton;
    boolean isShowingMasteryPanel = false;
    boolean isInRestorableMarket = false;
    float savedScrollerLocation = 0f;
    TooltipMakerAPI savedMasteryDisplay;
    Set<Integer> selectedMasteryButtons, activeMasteries;
    TooltipMakerAPI upgradeMasteryDisplay, confirmOrCancelDisplay;
    int currentMastery, maxMastery;

    public MasteryPanel(RefitHandler handler) {

        ReflectionUtils.GenericDialogData dialogData =
                ReflectionUtils.showGenericDialog("", Strings.DISMISS_WINDOW_STR, 900f, 600f);
        if (dialogData == null) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.CANT_OPEN_PANEL, Misc.getNegativeHighlightColor());
            return;
        }


        rootPanel = dialogData.panel;
        this.handler = handler;
        generateDialog(rootPanel, false, false);
    }

    public void forceRefresh(boolean variantChanged, boolean useSavedScrollerLocation) {
        if (rootPanel == null) return;

        handler.injectRefitScreen(variantChanged);
        if (useSavedScrollerLocation) {
            saveScrollerLocation();
        }

        generateDialog(rootPanel, true, useSavedScrollerLocation);
    }

    public void forceRefresh(boolean variantChanged) {
        forceRefresh(variantChanged, false);
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
        ship = handler.getSelectedShip();
        if (ship == null) {
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
        sModPanel = makeThisShipPanel(w, h - 100f, ship);
        masteryPanel = makeMasteryPanel(w, h - 100f, ship, useSavedScrollerLocation);
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

        int creditsAmt = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
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

        int masteryPointsAmt = (int) ShipMastery.getMasteryPoints(ship.getHullSpec());
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

    UIPanelAPI makeThisShipPanel(float width, float height, ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();

        List<HullModSpecAPI> applicableSpecs = new ArrayList<>();
        for (String id : variant.getHullSpec().getBuiltInMods()) {
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
            HullModEffect effect = spec.getEffect();
            if (effect.hasSModEffect() && !effect.isSModEffectAPenalty() &&
                    !variant.getSModdedBuiltIns().contains(id)) {
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
                                                   new SModTableRowPressed(this).getProxy());

        buildInList.addSpacer(7f);

        for (HullModSpecAPI spec : applicableSpecs) {
            addRowToHullModTable(buildInList, table, spec, !SModUtils.isHullmodBuiltIn(spec, ship.getVariant()));
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
        ReflectionUtils.setButtonListener(resetButton, new ClearSModsPressed(this, Strings.CLEAR_BUTTON_STR));
        if (variant.getSMods().size() == 0) {
            resetButton.setEnabled(false);
        }

        if (!TransientSettings.SMOD_REMOVAL_ENABLED) {
            ReflectionUtils.invokeMethod(resetButton, "setOpacity", 0f);
        }

        float modularCountW = 200f, modularCountH = 40f;
        int nSMods = variant.getSMods().size();
        int sModLimit = Misc.getMaxPermanentMods(ship);
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

    public void selectMasteryItem(int i) {
        selectedMasteryButtons.add(i);
        showUpgradeOrConfirmation();
    }

    public void deselectMasteryItem(int i) {
        selectedMasteryButtons.remove(i);
        showUpgradeOrConfirmation();
    }

    void showUpgradeOrConfirmation() {
        if (selectedMasteryButtons.equals(activeMasteries)) {
            ReflectionUtils.invokeMethod(upgradeMasteryDisplay, "setOpacity", currentMastery >= maxMastery ? 0f : 1f);
            ReflectionUtils.invokeMethod(confirmOrCancelDisplay, "setOpacity", 0f);
        }
        else {
            ReflectionUtils.invokeMethod(upgradeMasteryDisplay, "setOpacity", 0f);
            ReflectionUtils.invokeMethod(confirmOrCancelDisplay, "setOpacity", 1f);
        }
    }
    public Set<Integer> getSelectedMasteryButtons() {
        return selectedMasteryButtons;
    }

    UIPanelAPI makeMasteryPanel(float width, float height, ShipAPI ship, boolean useSavedScrollerLocation) {
        ShipHullSpecAPI hullSpec = ship.getHullSpec();
        final ShipHullSpecAPI baseHullSpec = Global.getSettings().getHullSpec(Utils.getBaseHullId(hullSpec));
        currentMastery = ShipMastery.getMasteryLevel(baseHullSpec);
        maxMastery = ShipMastery.getMaxMastery(baseHullSpec);

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
        new ConfirmOrCancelDisplay(new ConfirmMasteryChangesPressed(this), new CancelMasteryChangesPressed(this)).create(confirmOrCancelDisplay);
        masteryPanel.addUIElement(confirmOrCancelDisplay).belowMid(shipDisplay, 10f);

        ReflectionUtils.invokeMethod(confirmOrCancelDisplay, "setOpacity", 0f);

        float containerW = 500f, containerH = 450f;
        TooltipMakerAPI masteryContainer = masteryPanel.createUIElement(containerW, containerH, false);
        ButtonAPI containerOutline =
                masteryContainer.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                 Misc.getBrightPlayerColor(), containerW - 5f, containerH - 5f, 0f);
        containerOutline.setClickable(false);
        containerOutline.setGlowBrightness(0f);
        containerOutline.setMouseOverSound(null);
        masteryPanel.addUIElement(masteryContainer).inRMid(50f);

        float containerPadX = 4f, containerPadY = 8f;
        TooltipMakerAPI masteryDisplay =
                masteryPanel.createUIElement(containerW + 50f - containerPadX, containerH + 2f - containerPadY, true);

        float pad = 10f, minDescH = 80f, buttonW = 30f;
        float totalH = 0f, scrollToH = 0f;

        activeMasteries = ShipMastery.getActiveMasteriesCopy(baseHullSpec);
        selectedMasteryButtons = new HashSet<>(activeMasteries);
        for (int i = 1; i <= maxMastery; i++) {
            CustomPanelAPI descriptionPanel = Global.getSettings().createCustom(containerW + 50f, minDescH, null);
            TooltipMakerAPI description = descriptionPanel.createUIElement(containerW - 50f, minDescH, false);
            final List<MasteryEffect> effects = ShipMastery.getMasteryEffects(baseHullSpec, i);
            boolean alwaysShow = true;
            for (MasteryEffect effect : effects) {
                if (!MasteryUtils.alwaysShowDescription(effect)) {
                    alwaysShow = false;
                    break;
                }
            }
            boolean hidden = !alwaysShow && i > currentMastery + 1;

            if (!hidden) {
                description.addSpacer(20f);
                for (MasteryEffect effect : effects) {
                    description.setParaFont(Fonts.INSIGNIA_LARGE);
                    effect.getDescription().addLabel(description);
                    description.setParaFontDefault();
                    effect.addPostDescriptionSection(description);
                    description.addSpacer(20f);
                }
            }
            else {
                description.addPara("", 0f);
            }

            float descH = Math.max(minDescH, description.getHeightSoFar());
            descriptionPanel.getPosition().setSize(containerW - 15f, descH);

            TooltipMakerAPI descriptionButton = descriptionPanel.createUIElement(containerW, descH, false);
            ButtonAPI descOutline = descriptionButton.addAreaCheckbox(hidden ? Strings.UNKNOWN_EFFECT_STR : "", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                                Misc.getGrayColor(), containerW - 5f, descH, 0f);
            descOutline.setClickable(i <= currentMastery);
            descOutline.setGlowBrightness(i <= currentMastery ? 0.8f : 0.15f);
            if (i > currentMastery) {
                descOutline.setButtonPressedSound(null);
            }
            ReflectionUtils.setButtonListener(descOutline, new MasteryEffectButtonPressed(this, baseHullSpec, i));

            boolean hasTooltip = false;
            for (MasteryEffect effect : effects) {
                if (MasteryUtils.hasTooltip(effect)) {
                    hasTooltip = true;
                    break;
                }
            }

            if (hasTooltip && !hidden) {
                descriptionButton.addTooltipToPrevious(new TooltipMakerAPI.TooltipCreator() {
                    @Override
                    public boolean isTooltipExpandable(Object o) {
                        return false;
                    }

                    @Override
                    public float getTooltipWidth(Object o) {
                        return 500f;
                    }

                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        for (MasteryEffect effect : effects) {
                            effect.addTooltipIfHasTooltipTag(tooltip);
                        }
                    }
                }, TooltipMakerAPI.TooltipLocation.ABOVE, false);
            }

            TooltipMakerAPI levelButtonTTM = descriptionPanel.createUIElement(buttonW, descH, false);
            ButtonAPI levelButton = levelButtonTTM.addAreaCheckbox("" + i, null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                   Misc.getBrightPlayerColor(), buttonW, descH, 0f);
            levelButton.setClickable(false);
            levelButton.setGlowBrightness(0.3f);
            levelButton.setMouseOverSound(null);

            levelButton.highlight();

            if (i <= currentMastery) {
                descOutline.highlight();
                levelButton.setHighlightBrightness(0.5f);
                descOutline.setHighlightBrightness(0.25f);
            }
            else {
                levelButton.setHighlightBrightness(0.25f);
                descOutline.setHighlightBrightness(0f);
                levelButton.setEnabled(false);
            }

            // Instantly set the highlight strength to get rid of unwanted flickering when refreshing the panel
            ((Fader) ReflectionUtils.invokeMethod(descOutline, "getHighlightFader")).forceIn();
            ((Fader) ReflectionUtils.invokeMethod(levelButton, "getHighlightFader")).forceIn();

            if (activeMasteries.contains(i)) {
                descOutline.setChecked(true);
                descOutline.setHighlightBrightness(0f);
                levelButton.setChecked(true);
            }

            if (i <= currentMastery) {
                scrollToH = totalH;
            }

            // Cheap trick: to simulate darken effect, render the outline area checkbox above the text
            descriptionPanel.addUIElement(levelButtonTTM).inLMid(16f);
            if (i > currentMastery) {
                descriptionPanel.addUIElement(description).inLMid(75f);
                descriptionPanel.addUIElement(descriptionButton).inLMid(45f);
            }
            else {
                descriptionPanel.addUIElement(descriptionButton).inLMid(45f);
                descriptionPanel.addUIElement(description).inLMid(75f);
            }
            masteryDisplay.addComponent(descriptionPanel).inTL(0f, totalH);
            totalH += pad + descH;
        }
        masteryDisplay.setHeightSoFar(totalH - pad);
        masteryPanel.addUIElement(masteryDisplay).inTR(50f, 18f);

        if (useSavedScrollerLocation) {
            masteryDisplay.getExternalScroller().setYOffset(savedScrollerLocation);
        }
        else {
            masteryDisplay.getExternalScroller()
                          .setYOffset(Math.max(0f, Math.min(scrollToH - pad, totalH - containerH)));
        }
        savedMasteryDisplay = masteryDisplay;

        return masteryPanel;
    }

    public void saveScrollerLocation() {
        savedScrollerLocation = savedMasteryDisplay.getExternalScroller().getYOffset();
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
        String opCost = "" + (modular ? spec.getCostFor(variant.getHullSize()) : 0);
        int mpCost = SModUtils.getMPCost(spec, ship);
        String mpCostStr = "" + mpCost;
        int creditsCost = SModUtils.getCreditsCost(spec, ship);
        String creditsCostStr = Misc.getFormat().format(creditsCost);
        String modularString = modular ? Strings.YES_STR : Strings.NO_STR;
        Color masteryColor = Settings.MASTERY_COLOR;
        Color creditsColor = Misc.getHighlightColor();
        String cantBuildInReason = getCantBuildInReason(spec, mpCost, creditsCost);

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
                ShipAPI.HullSize hullSize = variant.getHullSize();
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

        TableRowData rowData = new TableRowData(spec.getId(), mpCost, creditsCost, cantBuildInReason);
        List<?> rows = (List<?>) ReflectionUtils.invokeMethod(table, "getRows");
        Object lastRow = rows.get(rows.size() - 1);
        ReflectionUtils.invokeMethodExtWithClasses(
                lastRow, "setData", false, new Class[]{Object.class}, rowData);
    }

    /**
     * Gives reason the mod can't be built in; returns null if hullmod can be built in
     */
    @Nullable String getCantBuildInReason(HullModSpecAPI spec, int mpCost, int creditsCost) {
        if (spec.hasTag(Tags.HULLMOD_NO_BUILD_IN) && !TransientSettings.IGNORE_NO_BUILD_IN_HULLMOD_IDS.contains(spec.getId())) {
            return spec.getDisplayName() + Strings.CANT_BUILD_IN_STR;
        }
        if (ship.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(ship) + TransientSettings.OVER_LIMIT_SMOD_COUNT.getModifiedInt()) {
            return Strings.LIMIT_REACHED_STR;
        }

        int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        int mp = (int) ShipMastery.getMasteryPoints(ship.getHullSpec());

        String notEnoughCredits = Strings.CREDITS_SHORTFALL_STR;
        String notEnoughMasteryPoints = Strings.MASTERY_POINTS_SHORTFALL_STR;
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
        } else {
            comparator = makeComparator(columnName);
        }
        currentColumnName = columnName;
        forceRefresh(false);
    }
}
