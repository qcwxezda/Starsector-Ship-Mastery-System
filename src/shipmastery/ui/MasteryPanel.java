package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.ui.UITable;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetPanelHandler;
import shipmastery.campaign.MasterySharingHandler;
import shipmastery.campaign.RefitHandler;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.ui.buttons.ButtonWithIcon;
import shipmastery.ui.buttons.CancelButton;
import shipmastery.ui.buttons.ConfirmButton;
import shipmastery.ui.buttons.HullReversionButton;
import shipmastery.ui.buttons.IntegrateButton;
import shipmastery.ui.buttons.LevelUpButton;
import shipmastery.ui.buttons.MasterySharingButton;
import shipmastery.ui.buttons.RerollButton;
import shipmastery.ui.buttons.SelectiveRestoreButton;
import shipmastery.ui.buttons.UseSPButton;
import shipmastery.ui.triggers.SModTableHeaderPressed;
import shipmastery.ui.triggers.SModTableRowPressed;
import shipmastery.ui.triggers.TabButtonPressed;
import shipmastery.util.ClassRefs;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.HullmodUtils;
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
    ButtonWithIcon upgradeButton, confirmButton, cancelButton, constructButton, rerollButton;
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
        sModPanel = makeThisShipPanel(w, h - 100f);
        masteryPanel = makeMasteryPanel(w, h - 100f, useSavedScrollerLocation, scrollToStart);
        togglePanelVisibility(!isInRestorableMarket || isShowingMasteryPanel ? masteryButton : sModButton);

        panel.addComponent(tabButtons).inTMid(0f);
        panel.addComponent(sModPanel).belowMid(tabButtons, 10f);
        panel.addComponent(masteryPanel).belowMid(tabButtons, 10f);
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
                    return 230f;
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

    void makeCurrencyLabels(CustomPanelAPI panel) {
        int creditsAmt = (int) Utils.getPlayerCredits().get();
        String creditsAmtFmt = Misc.getFormat().format(creditsAmt);
        String creditsString = Strings.MasteryPanel.creditsDisplay + creditsAmtFmt;

        int storyPointsAmt = Global.getSector().getPlayerStats().getStoryPoints();
        String storyPointsFmt = Misc.getFormat().format(storyPointsAmt);
        String storyPointsString = Strings.MasteryPanel.storyPointsDisplay + storyPointsFmt;

        String fullString = creditsString + "     "  + storyPointsString;

        var font = Fonts.INSIGNIA_LARGE;
        float fullStringWidth =
                Global.getSettings().computeStringWidth(fullString, font) + 10f;

        TooltipMakerAPI currency = panel.createUIElement(fullStringWidth, 30f, false);
        currency.setParaFont(font);
        LabelAPI label = currency.addPara(fullString, Misc.getGrayColor(), 10f);
        label.setAlignment(Alignment.LMID);
        label.setHighlight(creditsAmtFmt, storyPointsFmt);
        label.setHighlightColors(Misc.getHighlightColor(), Misc.getStoryBrightColor());

        panel.addUIElement(currency).inBMid(10f);
    }

    private void addShipPanelButtons(ShipAPI selectedShip, FleetMemberAPI member, CustomPanelAPI panel) {
        var spec = member.getHullSpec();
        int level = ShipMastery.getPlayerMasteryLevel(spec);
        int maxLevel = ShipMastery.getMaxMasteryLevel(spec);
        var selectedVariant = selectedShip.getVariant();

        var integrateButton = new IntegrateButton(usingSP, member, selectedVariant);
        addButton(integrateButton, panel, null, false, -38f, 25f);
        int integrateUnlockLevel = Math.min(maxLevel, MasteryUtils.UNLOCK_PSEUDOCORE_INTEGRATION_LEVEL);
        integrateButton.setEnabled(level >= integrateUnlockLevel, String.format(Strings.MasteryPanel.unlockAtLevel, integrateUnlockLevel));
        if (integrateButton.isEnabled() && selectedShip.getFleetMember() != member) {
            integrateButton.setEnabled(false, Strings.Misc.doesntAffectModules);
        }
        integrateButton.onFinish(() -> forceRefresh(true, true, true, false));

        var removeSModsButton = new HullReversionButton(usingSP, selectedShip);
        addButton(removeSModsButton, panel, null, false, -83f, 25f);
        int removalUnlockLevel = Math.min(maxLevel, MasteryUtils.UNLOCK_SMOD_REMOVAL_LEVEL);
        removeSModsButton.setEnabled(level >= removalUnlockLevel, String.format(Strings.MasteryPanel.unlockAtLevel, removalUnlockLevel));
        if (removeSModsButton.isEnabled() && Misc.getCurrSpecialMods(selectedVariant) == 0) {
            removeSModsButton.setEnabled(false, Strings.MasteryPanel.noSMods);
        }
        removeSModsButton.onFinish(() -> forceRefresh(true, true, true, false));

        var selectiveRestoreButton = new SelectiveRestoreButton(usingSP, selectedShip);
        addButton(selectiveRestoreButton, panel, null, false, -128f, 25f);
        int restoreUnlockLevel = Math.min(maxLevel, MasteryUtils.UNLOCK_SELECTIVE_RESTORATION_LEVEL);
        boolean unrestorable = selectedVariant.hasTag(Tags.VARIANT_UNRESTORABLE) || selectedVariant.getHullSpec().hasTag(Tags.HULL_UNRESTORABLE);
        selectiveRestoreButton.setEnabled(level >= restoreUnlockLevel, String.format(Strings.MasteryPanel.unlockAtLevel, restoreUnlockLevel));
        int numDMods = DModManager.getNumDMods(selectedVariant);
        if (selectiveRestoreButton.isEnabled()) {
            if (unrestorable) {
                selectiveRestoreButton.setEnabled(false, Strings.MasteryPanel.cantRestore);
            } else if (numDMods <= 0) {
                selectiveRestoreButton.setEnabled(false, Strings.MasteryPanel.noDMods);
            }
        }
        selectiveRestoreButton.onFinish(() -> {
            forceRefresh(true, true, true, false);
            if (DModManager.getNumDMods(selectedVariant) <= 0) {
                RefitHandler.setForceNullMemberAndDisableRestoreButton();
            }
        });

        var useSPButton = new UseSPButton();
        addButton(useSPButton, panel, null, false, -173f, 25f);
        useSPButton.setChecked(usingSP);
        useSPButton.onFinish(() -> {
            usingSP = !usingSP;
            forceRefresh(false, false, true, false);
        });
        useSPButton.isCheckbox = true;
    }

    UIPanelAPI makeThisShipPanel(float width, float height) {
        ShipVariantAPI moduleVariant = module.getVariant();
        hasLogisticBuiltIn = HullmodUtils.hasLogisticSMod(moduleVariant);
        hasLogisticEnhanceBonus = HullmodUtils.hasBonusLogisticSlot(moduleVariant);

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
            addRowToHullModTable(buildInList, table, spec, !HullmodUtils.isHullmodBuiltIn(spec, moduleVariant));
            buildInList.addImage(spec.getSpriteName(), 50f, tableEntryHeight - 6f, 6f);
        }

        buildInList.addTable(Strings.MasteryPanel.hullmodListsEmptyHint, -1, -buildInList.getHeightSoFar() + 10f);
        if (table.getRows().size() < 13) {
            table.autoSizeToRows(13);
        }

        addShipPanelButtons(module, root.getFleetMember(), thisShipPanel);

        int nSMods = Misc.getCurrSpecialMods(moduleVariant);
        var limit = getSModLimit(module);
        int sModLimit = limit.one;
        boolean limitEnhancedBySP = limit.two;
        String builtInText = Strings.MasteryPanel.builtInDisplay + String.format("%s/%s", nSMods, sModLimit);
        var font = Fonts.ORBITRON_24AABOLD;

        float modularCountW = Global.getSettings().computeStringWidth(builtInText, font) + 10f, modularCountH = 40f;
        //if (hasLogisticBuiltIn && hasLogisticEnhanceBonus) sModLimit++;
        TooltipMakerAPI modularCountTTM = thisShipPanel.createUIElement(modularCountW, modularCountH, false);
        modularCountTTM.setParaFont(font);
        LabelAPI modularCount = modularCountTTM.addPara(builtInText, Misc.getBrightPlayerColor(), 0f);
        modularCount.setAlignment(Alignment.RMID);
        modularCount.setHighlight("" + nSMods, "" + sModLimit);
        modularCount.setHighlightColors(Misc.getHighlightColor(),
                hasLogisticBuiltIn && hasLogisticEnhanceBonus || limitEnhancedBySP ?
                        Misc.getStoryBrightColor() :
                        Misc.getHighlightColor());

        thisShipPanel.addUIElement(buildInList).inTMid(37f);
        thisShipPanel.addUIElement(buildInListHeader).inTMid(20f);
        thisShipPanel.addUIElement(modularCountTTM).inBR(25f, -10f);
        makeCurrencyLabels(thisShipPanel);
        return thisShipPanel;
    }

    public Pair<Integer, Boolean> getSModLimit(ShipAPI ship) {
        int sModLimit = Math.max(0, Misc.getMaxPermanentMods(ship));
        if (ship.getVariant() == null) return new Pair<>(sModLimit, false);
        boolean limitEnhancedBySP = false;
//        if (usingSP) {
//            if (sModLimit < 1) {
//                sModLimit = 1;
//                limitEnhancedBySP = true;
//            }
//            if (module.getVariant().hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE) && sModLimit < 1 + EngineeringOverride.NUM_ADDITIONAL_SMODS) {
//                sModLimit = 1 + EngineeringOverride.NUM_ADDITIONAL_SMODS;
//                limitEnhancedBySP = true;
//            }
//        }
        return new Pair<>(sModLimit, limitEnhancedBySP);
    }

    void updateMasteryPanelButtons(ShipHullSpecAPI spec) {
        boolean changesPending = !Objects.equals(savedMasteryDisplay.getActiveLevels(), savedMasteryDisplay.getSelectedLevels());
        int level = ShipMastery.getPlayerMasteryLevel(spec);
        int maxLevel = ShipMastery.getMaxMasteryLevel(spec);
        boolean canUpgrade = MasteryUtils.hasEnoughXPToUpgradeOrEnhance(spec);
        upgradeButton.setEnabled(canUpgrade, MasteryUtils.getEnhanceCount(spec) >= MasteryUtils.MAX_ENHANCES
                ? Strings.MasteryPanel.maxReached
                : Strings.MasteryPanel.notEnoughXP);
        int constructLevel = Math.min(maxLevel, MasteryUtils.UNLOCK_MASTERY_SHARING_LEVEL);
        constructButton.setEnabled(level >= constructLevel, String.format(Strings.MasteryPanel.unlockAtLevel, constructLevel));
        int rerollLevel = Math.min(maxLevel, MasteryUtils.UNLOCK_REROLL_LEVEL);
        rerollButton.setEnabled(level >= rerollLevel, String.format(Strings.MasteryPanel.unlockAtLevel, rerollLevel));
        confirmButton.setEnabled(false, Strings.MasteryPanel.noChangesPending);
        cancelButton.setEnabled(false, Strings.MasteryPanel.noChangesPending);
        if (changesPending) {
            if (upgradeButton.isEnabled())
                upgradeButton.setEnabled(false, Strings.MasteryPanel.changesPending);
            if (constructButton.isEnabled())
                constructButton.setEnabled(false, Strings.MasteryPanel.changesPending);
            if (rerollButton.isEnabled())
                rerollButton.setEnabled(false, Strings.MasteryPanel.changesPending);
            confirmButton.setEnabled(true, null);
            cancelButton.setEnabled(true, null);
        }
    }

    public Map<Integer, String> getSelectedMasteryButtons() {
        return savedMasteryDisplay == null ? new HashMap<>() : savedMasteryDisplay.getSelectedLevels();
    }

    private void addButton(ButtonWithIcon button, CustomPanelAPI panel, @Nullable TooltipMakerAPI anchor, boolean anchorLeft, float extraXOffset, float extraYOffset) {
        var ttm = panel.createUIElement(button.width, button.height, false);
        button.create(ttm);
        PositionAPI pos;
        if (anchorLeft)
            if (anchor != null)
                pos = panel.addUIElement(ttm).belowLeft(anchor, 0f);
            else
                pos = panel.addUIElement(ttm).inTL(0f, 0f);
        else if (anchor != null)
            pos = panel.addUIElement(ttm).belowRight(anchor, 0f);
        else
            pos = panel.addUIElement(ttm).inTR(0f, 0f);
        pos.setXAlignOffset(extraXOffset);
        pos.setYAlignOffset(extraYOffset);
    }

    void addMasteryPanelButtons(CustomPanelAPI panel, FleetMemberAPI member, ShipHullSpecAPI restoredSpec, TooltipMakerAPI anchor) {
        int level = ShipMastery.getPlayerMasteryLevel(restoredSpec);
        int maxLevel = ShipMastery.getMaxMasteryLevel(restoredSpec);
        boolean isEnhance = level >= maxLevel;
        upgradeButton = new LevelUpButton(member, restoredSpec, isEnhance);
        addButton(upgradeButton, panel, anchor, true, 0f, -30f);
        upgradeButton.onFinish(() -> forceRefresh(true, false, isEnhance, false));
        constructButton = new MasterySharingButton(restoredSpec);
        addButton(constructButton, panel, anchor, true, 45f, -30f);
        constructButton.setChecked(MasterySharingHandler.isMasterySharingActive(restoredSpec));
        constructButton.onFinish(() -> forceRefresh(true, false, true, false));
        constructButton.isCheckbox = true;
        rerollButton = new RerollButton(restoredSpec);
        addButton(rerollButton, panel, anchor, true, 90f, -30f);
        rerollButton.onFinish(() -> forceRefresh(true, false, true, false));
        confirmButton = new ConfirmButton(restoredSpec, getSelectedMasteryButtons());
        addButton(confirmButton, panel, anchor, false, -49f, -30f);
        confirmButton.onFinish(() -> forceRefresh(true, true, true, false));
        cancelButton = new CancelButton();
        addButton(cancelButton, panel, anchor, false, -4f, -30f);
        cancelButton.onFinish(() -> forceRefresh(false, false, true, false));
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

        var progressBarPlugin = new FleetPanelHandler.FleetPanelItemUIPlugin(
                root.getVariant(),
                root.getFleetMember(),
                restoredHullSpec,
                shipDisplay.getPosition(), () -> forceRefresh(true, false, false, false));
        progressBarPlugin.heightOverride = 16f;
        progressBarPlugin.numBars = 80;
        progressBarPlugin.widthOverride = shipDisplaySize - 5f;
        progressBarPlugin.extraXOffset = -5f;
        progressBarPlugin.extraYOffset = shipDisplaySize - 10f;
        progressBarPlugin.showIcons = false;
        CustomPanelAPI progressBar = Global.getSettings().createCustom(shipDisplaySize, shipDisplaySize + 25f, progressBarPlugin);
        progressBarPlugin.makeOutline(progressBar, false, false);
        masteryPanel.addComponent(progressBar).inTL(50f, 90f);

        float containerW = 800f, containerH = height - 66f;
        TooltipMakerAPI masteryContainer = masteryPanel.createUIElement(containerW, containerH + 2f, false);
        new MasteryDisplayOutline(containerW, containerH + 2f).create(masteryContainer);
        masteryPanel.addUIElement(masteryContainer).inTR(51f, 17f);

        float containerPadX = 4f, containerPadY = 8f;
        float masteryDisplayWidth = containerW + 50f - containerPadX, masteryDisplayHeight = containerH + 2f - containerPadY;
        float pad = 120f;

        MasteryDisplay display = new MasteryDisplay(
                this,
                module.getVariant(),
                root.getFleetMember(),
                restoredHullSpec,
                module.getFleetMember() != root.getFleetMember(),
                containerW,
                containerH,
                pad,
                !useSavedScrollerLocation,
                () -> updateMasteryPanelButtons(restoredHullSpec));
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
            display.scrollToHeight(Math.max(0f, Math.min(savedMasteryDisplay.getSavedScrollerHeight(), display.getTotalHeight() - containerH - pad + 6f)));
        }
        if (!useSavedScrollerLocation) {
            if (scrollToStart) {
                display.scrollToLevel(1, savedMasteryDisplay == null);
            } else {
                display.scrollToLevel(display.getLevelToScrollTo(), savedMasteryDisplay == null);
            }
        }

        savedMasteryDisplay = display;

        float buttonW = 40f, buttonH = 35f, buttonPad = 25f;
        int maxButtons = (int) (containerW / (buttonW + buttonPad));
        int buttonsPerRow;
        int numButtons;
        if (maxButtons < maxMastery) {
            buttonW = 28f;
            buttonH = 24f;
            buttonPad = 4f;
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
        masteryPanel.addComponent(masteryButtonsPanel).inTR(54f - buttonPad / 2f, containerH + 25f + buttonH);

        int unassignedLevels = MasteryUtils.getPlayerUnassignedCount(root.getHullSpec());
        if (unassignedLevels > 0) {
            TooltipMakerAPI unassignedWarning = masteryPanel.createUIElement(300f, 20f, false);
            String warningText = unassignedLevels == 1 ? Strings.MasteryPanel.unassignedWarningTextSingular : Strings.MasteryPanel.unassignedWarningTextPlural;
            String warningTextFmt = String.format(warningText, unassignedLevels);
            LabelAPI warningLabel = unassignedWarning.addPara(warningText, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, "" + unassignedLevels);
            warningLabel.setColor(Misc.getGrayColor());
            masteryPanel.addUIElement(unassignedWarning).inTR(-240f + warningLabel.computeTextWidth(warningTextFmt), -10f);
        }

        addMasteryPanelButtons(masteryPanel, root.getFleetMember(), restoredHullSpec, shipDisplay);
        updateMasteryPanelButtons(restoredHullSpec);

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
        int creditsCost = HullmodUtils.getBuildInCost(spec, module, usingSP);
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

        tableTTM.addTooltipToAddedRow(
                new HullmodUtils.HullmodTooltipCreator(spec, module) {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        super.createTooltip(tooltip, expanded, tooltipParam);
                        var color = cantBuildInReason != null ? Misc.getGrayColor() : usingSP ? Misc.getStoryBrightColor() : Misc.getHighlightColor();
                        tooltip.addPara(Strings.MasteryPanel.doubleClickHint, color, 10f);
                    }
                },
                TooltipMakerAPI.TooltipLocation.BELOW,
                false);

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
        if (Misc.getCurrSpecialMods(module.getVariant()) >= getSModLimit(module).one
                + TransientSettings.OVER_LIMIT_SMOD_COUNT.getModifiedInt() + logisticsEnhanceBonus && modular) {
            return Strings.MasteryPanel.limitReached;
        }

        int credits = (int) Utils.getPlayerCredits().get();
        int sp = Global.getSector().getPlayerStats().getStoryPoints();

        String notEnoughCredits = Strings.MasteryPanel.notEnoughCredits;
        String notEnoughStoryPoints = Strings.Misc.noStoryPoints;

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
            return HullmodUtils.getBuildInCost(spec, module);
        }

        if (columnNames[5].equals(columnName)) {
            return !HullmodUtils.isHullmodBuiltIn(spec, module.getVariant());
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
}
