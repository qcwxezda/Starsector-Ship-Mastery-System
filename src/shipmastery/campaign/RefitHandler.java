package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.listeners.CharacterStatsRefreshListener;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.coreui.refit.ModPickerDialogV3;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.campaign.listeners.CoreTabListener;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.plugin.SModAutofitCampaignPlugin;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.ui.triggers.MasteryButtonPressed;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class RefitHandler implements CoreTabListener, CharacterStatsRefreshListener {
    boolean insideRefitPanel = false;
    static final String MASTERY_BUTTON_TAG = "sms_mastery_button_tag";
    static final Logger logger = Logger.getLogger(RefitHandler.class);
    public static final String CURRENT_REFIT_SHIP_KEY = "$sms_CurrentRefitShip";

    // Keep track of added panels to remove them in later inject calls
    WeakReference<UIPanelAPI> masteryButtonPanelRef = new WeakReference<>(null);

    // Keep track of the current ship's hull spec and active mastery set in the refit panel
    // Use this info to determine when to call applyEffectsOnBeginRefit and unapplyEffectsOnBeginRefit
    ShipInfo currentShipInfo = new ShipInfo(null, null);

    private ShipAPI lastSelectedRealShip;

    public RefitHandler() {
        Global.getSector().registerPlugin(new SModAutofitCampaignPlugin(this));
    }

    /** (Currently selected module, last selected ship with a fleet member attached)
     *  (null, null) if not inside the refit screen */
    public Pair<ShipAPI, ShipAPI> getSelectedShip(UIPanelAPI coreUI) {
        if (!insideRefitPanel) {
            return new Pair<>(null, null);
        }
        ShipAPI ship;
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI, "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object shipDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getShipDisplay");
            ship = (ShipAPI) ReflectionUtils.invokeMethodNoCatch(shipDisplay, "getShip");
        } catch (Exception e) {
            return new Pair<>(null, null);
        }

        if (ship != null && ship.getFleetMember() != null && ship.getFleetMember().getShipName() != null) {
            lastSelectedRealShip = ship;
            return new Pair<>(ship, ship);
        }
        return new Pair<>(ship, lastSelectedRealShip);
    }

    public static String restoreButtonFieldName;
    public static void setForceNullMemberAndDisableRestoreButton() {
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(ReflectionUtils.getCoreUI(), "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object shipDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getShipDisplay");
            Object designDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getDesignDisplay");
            Object member = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getMember");
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "setForceSetMemberNextCall", true);
            ReflectionUtils.invokeMethodNoCatchExtWithClasses(
                    shipDisplay,
                    "setFleetMember",
                    false,
                    new Class<?>[] {FleetMember.class, HullVariantSpec.class},
                    new Object[] {member, null});
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "setForceSetMemberNextCall", false);
            if (designDisplay == null) return;

            if (restoreButtonFieldName == null) {
                for (Field field : designDisplay.getClass().getDeclaredFields()) {
                    if (ButtonAPI.class.isAssignableFrom(field.getType())) {
                        ButtonAPI button = (ButtonAPI) ReflectionUtils.getFieldNoCatch(designDisplay, field.getName());
                        var shortcut = ReflectionUtils.invokeMethodNoCatch(button, "getShortcut");
                        if (!(shortcut instanceof Enum<?> e)) continue;
                        if ("REFIT_RESTORE".equals(e.name())) {
                            restoreButtonFieldName = field.getName();
                            break;
                        }
                    }
                }
            }
            if (restoreButtonFieldName != null) {
                var restoreButton = ReflectionUtils.getFieldNoCatch(designDisplay, restoreButtonFieldName);
                var array = Array.newInstance(restoreButton.getClass(), 1);
                Array.set(array, 0, restoreButton);
                ReflectionUtils.invokeMethodNoCatch(designDisplay, "disable", array);
            }
        } catch (Exception e) {
            logger.error("[Ship Mastery System] Failed to sync refit screen with variant", e);
        }
    }

    public static void syncRefitScreenWithVariant(boolean saveVariant) {
        try {
            // This is necessary if allowing mastery panel to be open while in the menu for adding hullmods
            // ((ModWidget) getModsPanel()).syncWithCurrentVariant((HullVariantSpec) getSelectedShip().one);
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(ReflectionUtils.getCoreUI(), "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");

            Boolean isEditedSinceSave = (Boolean) ReflectionUtils.invokeMethodNoCatch(refitPanel, "isEditedSinceSave");
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "syncWithCurrentVariant", true);
            Object opDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getOpDisplay");
            Object shipDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getShipDisplay");
            Object currentVariant = ReflectionUtils.invokeMethodNoCatch(shipDisplay, "getCurrentVariant");
            ReflectionUtils.invokeMethodNoCatch(opDisplay, "syncWithVariant", currentVariant);

            ReflectionUtils.invokeMethodNoCatch(refitPanel, "setEditedSinceSave", isEditedSinceSave);

            if (saveVariant) {
                // save the variant so can't undo s-mods
                ReflectionUtils.invokeMethodNoCatch(refitPanel, "saveCurrentVariant");
                // To disable the undo button
                ReflectionUtils.invokeMethodNoCatch(refitPanel, "setEditedSinceSave", false);
            }
        } catch (Exception e) {
            logger.error("[Ship Mastery System] Failed to sync refit screen with variant", e);
        }
    }

    /**
     * Multiple mods panels can appear in two ways:
     * - If adding hullmods, a separate mods panel is generated (different from getModDisplay.getMods)
     * - In rare instances, rapid clicking of the Add button can generate multiple separate mods panels
     */
    public LinkedList<UIPanelAPI> getAllModsPanels(UIPanelAPI coreUI) {
        LinkedList<UIPanelAPI> panelList = new LinkedList<>();
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI, "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object modDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getModDisplay");
            panelList.add((UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(modDisplay, "getMods"));

            // The screen for adding hull mods has a different mod display object for some reason
            List<?> coreChildren = (List<?>) ReflectionUtils.invokeMethod(coreUI, "getChildrenNonCopy");
            for (Object child : coreChildren) {
                if (child instanceof ModPickerDialogV3) {
                    List<?> subChildren = (List<?>) ReflectionUtils.invokeMethod(child, "getChildrenNonCopy");
                    for (Object subChild : subChildren) {
                        if (modDisplay != null && subChild.getClass().equals(modDisplay.getClass())) {
                            panelList.add((UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(subChild, "getMods"));
                        }
                    }
                }
            }

            return panelList;
        } catch (Exception e) {
            logger.error("[Ship Mastery System] Failed to grab refit screen mods panels", e);
            return new LinkedList<>();
        }
    }

    public UIPanelAPI getModsPanel(UIPanelAPI coreUI) {
        return getAllModsPanels(coreUI).getLast();
    }

    void modifyBuildInButton(UIPanelAPI coreUI) {
        // Modify the "build-in" button
        try {
            LinkedList<UIPanelAPI> modsPanels = getAllModsPanels(coreUI);
            Object modsPanel = modsPanels.getLast();
            final ButtonAPI addButton = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(modsPanel, "getAdd");
            final ButtonAPI permButton = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(modsPanel, "getPerm");

            // Modify every button that isn't the mastery or build-in buttons to trigger a refresh on click
            // This should just be the "go back" and "add" buttons
            List<?> children = (List<?>) ReflectionUtils.invokeMethod(modsPanel, "getChildrenNonCopy");
            for (Object child : children) {
                if (child instanceof ButtonAPI button && child != permButton && !MASTERY_BUTTON_TAG.equals(
                        button.getCustomData())) {
                    final Object origListener = ReflectionUtils.getButtonListener(button);
                    if (!Proxy.isProxyClass(origListener.getClass())) {
                        ReflectionUtils.setButtonListener(button, new ActionListener() {
                            @Override
                            public void trigger(Object... args) {
                                DeferredActionPlugin.performLater(() -> injectRefitScreen(false, false, button == addButton), 0f);
                                ReflectionUtils.invokeMethodExtWithClasses(origListener, "actionPerformed", false,
                                                                           new Class[]{Object.class, Object.class},
                                                                           args);
                            }
                        });
                    }
                }
            }

            // Delete the build-in button from all mods panels, including inactive ones
            for (UIPanelAPI panel : modsPanels) {
                ButtonAPI perm = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(panel, "getPerm");
                if (perm != null) {
                    perm.setOpacity(0f);
                }
            }
        } catch (Exception e) {
            logger.error("[Ship Mastery System] Failed to modify build-in button", e);
        }
    }

    public static class ButtonFlasherPlugin implements CustomUIPanelPlugin {
        private ButtonAPI button;
        private final float flashTime = 0.6f;
        private final IntervalUtil flashInterval = new IntervalUtil(flashTime, flashTime);

        @Override
        public void positionChanged(PositionAPI position) {
            flashInterval.forceIntervalElapsed();
        }

        @Override
        public void renderBelow(float alphaMult) {}

        @Override
        public void render(float alphaMult) {}

        @Override
        public void advance(float amount) {
            if (button == null) return;
            flashInterval.advance(amount);
            if (flashInterval.intervalElapsed()) {
                button.flash(false, flashTime/2f, flashTime/2f);
            }
        }

        @Override
        public void processInput(List<InputEventAPI> events) {}

        @Override
        public void buttonPressed(Object buttonId) {}
    }

    void updateMasteryButton(UIPanelAPI coreUI, boolean shouldHide) {
        UIPanelAPI modsPanel = getModsPanel(coreUI);

        // Remove existing CustomPanelAPIs, which would be mastery buttons we added previously
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(modsPanel, "getChildrenNonCopy");
        children.removeIf(child -> child == masteryButtonPanelRef.get());

        if (shouldHide) return;

        ButtonAPI permButton = (ButtonAPI) ReflectionUtils.invokeMethod(modsPanel, "getPerm");
        Pair<ButtonAPI, CustomPanelAPI> masteryButtonPair = ReflectionUtils.makeButton(
                Strings.RefitScreen.masteryButton, new ButtonFlasherPlugin(), new MasteryButtonPressed(this),
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM,
                permButton.getPosition().getWidth(), permButton.getPosition().getHeight(), Keyboard.KEY_Q);
        ButtonAPI masteryButton = masteryButtonPair.one;
        masteryButton.setCustomData(MASTERY_BUTTON_TAG);
        ((ButtonFlasherPlugin) (masteryButtonPair.two.getPlugin())).button =  masteryButton;
        UIPanelAPI masteryButtonPanel = masteryButtonPair.two;

        modsPanel.addComponent(masteryButtonPanel).belowMid(permButton, -25f).setXAlignOffset(-5f);
        masteryButtonPanelRef = new WeakReference<>(masteryButtonPanel);
    }

    void addMPDisplay(UIPanelAPI coreUI) {
        UIPanelAPI currentTab = (UIPanelAPI) ReflectionUtils.invokeMethod(coreUI, "getCurrentTab");
        // Add MP display to the ship displays on the left side of the refit screen
        try {
            //noinspection unchecked
            Map<ButtonAPI, FleetMember> buttonToMemberMap = (Map<ButtonAPI, FleetMember>) ReflectionUtils.invokeMethodNoCatch(
                    currentTab, "getButtonToMember");
            if (buttonToMemberMap != null && !buttonToMemberMap.isEmpty()) {
                UIPanelAPI scroller = (UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(
                        ReflectionUtils.invokeMethodNoCatch(currentTab, "getFleetList"),
                        "getScroller");
                UIPanelAPI container =  (UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(scroller, "getContentContainer");

                List<?> sortedButtonList = (List<?>) ReflectionUtils.invokeMethodNoCatch(container, "getChildrenNonCopy");
                sortedButtonList.removeIf(child -> child instanceof CustomPanelAPI panel && panel.getPlugin() instanceof FleetPanelHandler.FleetPanelItemUIPlugin);

                buttonToMemberMap.forEach((button, member) -> {
                    if (member.getShipName() == null) return;
                    var pos = button.getPosition();
                    var w = pos.getWidth();
                    var h = pos.getHeight();
                    var plugin = new FleetPanelHandler.FleetPanelItemUIPlugin(member, pos);
                    plugin.heightOverride = 40f;
                    plugin.numBars = 20;
                    plugin.extraYOffset = -10f;
                    plugin.extraXOffset = -3f;
                    CustomPanelAPI custom = Global.getSettings().createCustom(w, h, plugin);
                    plugin.makeOutline(custom, true);
                    scroller.addComponent(custom).setYAlignOffset(pos.getY() - container.getPosition().getY());
                });

//                List<?> sortedButtonList = (List<?>) ReflectionUtils.invokeMethodNoCatch(parent, "getChildrenNonCopy");
//                // Delete MP panels we may have previously added
//                sortedButtonList.removeIf(child -> child == mpPanelRef.get());
//                float w = parent.getPosition().getWidth(), h = parent.getPosition().getHeight();
//                CustomPanelAPI custom = Global.getSettings().createCustom(w, h, null);
//
//                TooltipMakerAPI tooltipMaker = custom.createUIElement(w, h, false);
//                tooltipMaker.setParaFontColor(Settings.MASTERY_COLOR);
//                for (int i = 0; i < sortedButtonList.size(); i++) {
//                    // Should be all buttons, but the item we add isn't a button so technically the list can contain non-buttons...
//                    if (!(sortedButtonList.get(i) instanceof ButtonAPI button)) continue;
//                    float h2 = tooltipMaker.getHeightSoFar();
//                    //noinspection SuspiciousMethodCalls
//                    FleetMemberAPI fm = buttonToMemberMap.get(sortedButtonList.get(i));
//                    if (fm != null) {
//                        ShipHullSpecAPI spec = fm.getHullSpec();
//                        int currentMastery = ShipMastery.getPlayerMasteryLevel(spec);
//                        int maxMastery = ShipMastery.getMaxMasteryLevel(spec);
//                        boolean padded = false;
//                        if (currentMastery < maxMastery) {
//                            boolean canLevelUp = ShipMastery.getPlayerMasteryPoints(spec) >= MasteryUtils.getUpgradeCost(spec);
//                            String canLevelUpStr = "+";
//                            tooltipMaker.addPara(
//                                    Strings.RefitScreen.masteryLabel + String.format("%s/%s", currentMastery, maxMastery) + " " + (canLevelUp ? canLevelUpStr : ""),
//                                    10f,
//                                    Settings.POSITIVE_HIGHLIGHT_COLOR,
//                                    canLevelUpStr).setAlignment(Alignment.LMID);
//                            padded = true;
//                        }
//                        int mp = (int) ShipMastery.getPlayerMasteryPoints(spec);
//                        if (mp > 0) {
//                            tooltipMaker.addPara(
//                                    mp + " MP",
//                                    padded ? 0f : 10f).setAlignment(Alignment.LMID);
//                            padded = true;
//                        }
//                        int enhanceCount = MasteryUtils.getEnhanceCount(spec);
//                        if (enhanceCount > 0) {
//                            String enhanceStr = "+" + enhanceCount;
//                            tooltipMaker.addPara(enhanceStr, padded ? 0f : 10f, Misc.getStoryBrightColor(), enhanceStr).setAlignment(Alignment.LMID);
//                        }
//                    }
//                    float hDiff = tooltipMaker.getHeightSoFar() - h2;
//                    if (i < sortedButtonList.size() - 1 && sortedButtonList.get(i + 1) instanceof ButtonAPI nextButton) {
//                        tooltipMaker.addSpacer(button.getPosition().getY() - nextButton.getPosition().getY() - hDiff);
//                    }
//                }
//                custom.addUIElement(tooltipMaker);
//                //parent.addComponent(custom).inBL(2f, -6f);
//                mpPanelRef = new WeakReference<>(custom);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void injectRefitScreen(boolean shouldSync) {
        injectRefitScreen(shouldSync, false);
    }

    public void injectRefitScreen(boolean shouldSync, boolean shouldSaveIfSyncing) {
        injectRefitScreen(shouldSync, shouldSaveIfSyncing, false);
    }

    public void injectRefitScreen(boolean shouldSync, final boolean shouldSaveIfSyncing, boolean hideMasteryButton) {
        //coreUI = new WeakReference<>((CoreUIAPI) ReflectionUtils.getCoreUI());
        UIPanelAPI coreUI = ReflectionUtils.getCoreUI();

        modifyBuildInButton(coreUI);
        updateMasteryButton(coreUI, hideMasteryButton);

        if (Settings.SHOW_MP_AND_LEVEL_IN_REFIT) {
            addMPDisplay(coreUI);
        }

        if (shouldSync) {
            checkIfRefitShipChanged(coreUI);
            syncRefitScreenWithVariant(shouldSaveIfSyncing);
        }
    }

    /** This is called every time the active ship in the refit screen changes. */
    @Override
    public void reportAboutToRefreshCharacterStatEffects() {}

    /** This is called every time the active ship in the refit screen changes. */
    @Override
    public void reportRefreshedCharacterStatEffects() {
        if (insideRefitPanel) {
            checkIfRefitShipChanged(ReflectionUtils.getCoreUI());
        }
    }

    void checkIfRefitShipChanged(UIPanelAPI coreUI) {
        Pair<ShipAPI, ShipAPI> moduleAndRoot = getSelectedShip(coreUI);
        final ShipAPI module = moduleAndRoot.one;
        ShipAPI root = moduleAndRoot.two;

        // Set the (temporary) "current-refit-ship" memory item
        var memory = Global.getSector().getPlayerFleet().getMemoryWithoutUpdate();
        memory.set(CURRENT_REFIT_SHIP_KEY, moduleAndRoot.one, 0f);

        if (module != null) {
            // bypass the arbitrary checks in removeMod since we're adding it back anyway
            //String lastHullmodId = Utils.getLastHullModId(module.getVariant());
            //if (!"sms_mastery_handler".equals(lastHullmodId)) {
                module.getVariant().getHullMods().remove("sms_mastery_handler");
                module.getVariant().getHullMods().add("sms_mastery_handler");
                //shouldSync = true;
            //}
            // This call does nothing except set variant.hasOpAffectingMods = null, which
            // triggers the variant to refresh its statsForOpCosts
            module.getVariant().addPermaMod("sms_mastery_handler");
        }

        ShipInfo newShipInfo = new ShipInfo(module, root);
        if (!Objects.equals(currentShipInfo, newShipInfo)) {
//            System.out.println(
//                    "Refit ship changed: " + (
//                            (currentShipInfo.rootSpec == null ? "null" : currentShipInfo.rootSpec.getHullId()) + ", " + (currentShipInfo.moduleVariant == null ? "null" :currentShipInfo.moduleVariant.getHullVariantId())) + " -> " +
//                            (newShipInfo.rootSpec == null ? "null" : newShipInfo.rootSpec.getHullId()) + ", " + (newShipInfo.moduleVariant == null ? "null" :newShipInfo.moduleVariant.getHullVariantId()));
            onRefitScreenShipChanged(newShipInfo);
            currentShipInfo = newShipInfo;
        }
    }

    final List<EffectActivationRecord> effectsToDeactivate = new ArrayList<>();
    void onRefitScreenShipChanged(ShipInfo newInfo) {
        final ShipHullSpecAPI newSpec = newInfo.rootSpec;
        final ShipVariantAPI newVariant = newInfo.moduleVariant;

        // Unset the hidden tag from the Engineering Override hullmod
        // Have to do it here, as when it's hidden none of the hullmod check-if-applicable methods get called
        Global.getSettings().getHullModSpec(Strings.Hullmods.ENGINEERING_OVERRIDE).setHidden(false);

        for (int i = effectsToDeactivate.size() - 1; i >= 0; i--) {
            EffectActivationRecord toDeactivate = effectsToDeactivate.get(i);
            toDeactivate.effect.onEndRefit(toDeactivate.moduleVariant, toDeactivate.isModule);
            //System.out.println("Unapply: " + toDeactivate.id + " to " + toDeactivate.moduleVariant.getHullVariantId() + ", " + toDeactivate.isModule);
        }

        effectsToDeactivate.clear();

        if (newSpec != null && newVariant != null) {
            MasteryUtils.applyMasteryEffects(
                newSpec, newInfo.activeMasteries, false, effect -> {
                    boolean isModule = !Objects.equals(Utils.getRestoredHullSpecId(newVariant.getHullSpec()), newSpec.getHullId());
                    if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                        effect.onBeginRefit(newVariant, isModule);
                        //System.out.println("Apply: " + id + " to " + newVariant.getHullVariantId() + ", " + isModule);
                        effectsToDeactivate.add(new EffectActivationRecord(effect, newVariant, isModule));
                    }
                });
        }

        //System.out.println("-------------");
    }

    @Override
    public void onCoreTabOpened(CoreUITabId id) {
        if (id != CoreUITabId.REFIT) {
            insideRefitPanel = false;
            checkIfRefitShipChanged(ReflectionUtils.getCoreUI());
            return;
        }
        injectRefitScreen(false);
        checkIfRefitShipChanged(ReflectionUtils.getCoreUI());
        insideRefitPanel = true;
    }

    @Override
    public void onCoreUIDismissed() {
        insideRefitPanel = false;
        checkIfRefitShipChanged(ReflectionUtils.getCoreUI());
    }

    record EffectActivationRecord(MasteryEffect effect, ShipVariantAPI moduleVariant, boolean isModule) {}

    static class ShipInfo {
        /** Hull spec of the root ship (so parent if ship is a module) */
        final ShipHullSpecAPI rootSpec;

        /** Currently selected module variant */
        final ShipVariantAPI moduleVariant;

        /** Active masteries at the time of selection -- if these change, need to refresh the ship */
        final NavigableMap<Integer, String> activeMasteries;

        ShipInfo(ShipAPI moduleShip, ShipAPI rootShip) {
            if (rootShip == null) {
                rootSpec = null;
                activeMasteries = new TreeMap<>();
            }
            else {
                rootSpec = Utils.getRestoredHullSpec(rootShip.getHullSpec());
                activeMasteries = ShipMastery.getPlayerActiveMasteriesCopy(rootSpec);
            }
            moduleVariant = moduleShip == null ? null : moduleShip.getVariant();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ShipInfo o)) return false;
            boolean sameRootSpec = (rootSpec == null && o.rootSpec == null)
                    || (rootSpec != null && o.rootSpec != null && rootSpec.getHullId().equals(o.rootSpec.getHullId()));
            boolean sameModuleVariant = moduleVariant == o.moduleVariant;//(moduleVariant == null && o.moduleVariant == null)
                    //|| (moduleVariant != null && o.moduleVariant != null && moduleVariant.getHullVariantId().equals(o.moduleVariant.getHullVariantId()));
            boolean sameActiveMasteries = Objects.equals(activeMasteries, o.activeMasteries);
            return sameRootSpec && sameModuleVariant && sameActiveMasteries;
        }

        @Override
        public int hashCode() {
            int i = activeMasteries.hashCode();
            if (rootSpec != null) i += 31 * rootSpec.getHullId().hashCode() + 17;
            if (moduleVariant != null) i += 31 * moduleVariant.hashCode() + 17;
            return i;
        }
    }
}
