package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CharacterStatsRefreshListener;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.coreui.refit.ModPickerDialogV3;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.ui.triggers.MasteryButtonPressed;
import shipmastery.util.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

public class RefitHandler implements CoreUITabListener, EveryFrameScript, CharacterStatsRefreshListener {
    boolean isFirstFrame = true;
    WeakReference<CoreUIAPI> coreUI = new WeakReference<>(null);
    float epsilonTime = 0.00001f;
    boolean insideRefitPanel = false;
    static final String MASTERY_BUTTON_TAG = "sms_mastery_button_tag";

    // Keep track of added panels to remove them in later inject calls
    WeakReference<UIPanelAPI> mpPanelRef = new WeakReference<>(null), masteryButtonPanelRef = new WeakReference<>(null);

    // Keep track of the current ship's hull spec and active mastery set in the refit panel
    // Use this info to determine when to call applyEffectsOnBeginRefit and unapplyEffectsOnBeginRefit
    ShipInfo currentShipInfo = new ShipInfo(null, null);

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float v) {
        if (isFirstFrame) {
            // Since the coreUI's "screenPanel" isn't created on the first frame, trying to do anything with the UI
            // on the first frame will cause an NPE. Therefore, we will initialize the screenPanel before trying
            // to call findAllClasses, if it hasn't been initialized already.
            try {
                CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
                Field field = campaignUI.getClass().getDeclaredField("screenPanel");
                field.setAccessible(true);
                if (field.get(campaignUI) == null) {
                    field.set(campaignUI, field.getType().getConstructor(float.class, float.class)
                                               .newInstance(
                                                       Global.getSettings().getScreenWidth(),
                                                       Global.getSettings().getScreenHeight()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isFirstFrame = false;
        }

        if (!ClassRefs.foundAllClasses()) {
            Global.getSector().getCampaignUI().setDisallowPlayerInteractionsForOneFrame();
            ClassRefs.findAllClasses();
        }

        if (!insideRefitPanel || Global.getSector() == null || Global.getSector().getCampaignUI() == null) return;

        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!CoreUITabId.REFIT.equals(ui.getCurrentCoreTab())) {
            insideRefitPanel = false;
        }

        // Due to a bug, if the player ESCs out of the refit screen in a market, the core tab is still shown as REFIT
        // even though it's been closed. To combat this, check if the savedOptionList is empty. If it is, we're still
        // in the refit screen; otherwise, we've ESCed out of the refit screen.
        else if (ui.getCurrentInteractionDialog() != null
                && ui.getCurrentInteractionDialog().getOptionPanel() != null
                && !ui.getCurrentInteractionDialog().getOptionPanel().getSavedOptionList().isEmpty()) {
            insideRefitPanel = false;
        }

        if (!insideRefitPanel) {
            checkIfRefitShipChanged();
        }
    }

    private ShipAPI lastSelectedRealShip;
    /** (Currently selected module, last selected ship with a fleet member attached)
     *  (null, null) if not inside the refit screen */
    public Pair<ShipAPI, ShipAPI> getSelectedShip() {
        if (!insideRefitPanel) {
            return new Pair<>(null, null);
        }
        ShipAPI ship;
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI.get(), "getCurrentTab");
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

    void syncRefitScreenWithVariant(boolean saveVariant) {
        try {
            // This is necessary if allowing mastery panel to be open while in the menu for adding hullmods
            // ((ModWidget) getModsPanel()).syncWithCurrentVariant((HullVariantSpec) getSelectedShip().one);
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI.get(), "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "syncWithCurrentVariant");


            if (saveVariant) {
                // save the variant so can't undo s-mods
                ReflectionUtils.invokeMethodNoCatch(refitPanel, "saveCurrentVariant");
                // To disable the undo button
                ReflectionUtils.invokeMethodNoCatch(refitPanel, "setEditedSinceSave", false);
            }
        } catch (Exception ignore) {
        }
    }

    /**
     * Multiple mods panels can appear in two ways:
     * - If adding hullmods, a separate mods panel is generated (different from getModDisplay.getMods)
     * - In rare instances, rapid clicking of the Add button can generate multiple separate mods panels
     */
    public LinkedList<UIPanelAPI> getAllModsPanels() {
        if (coreUI.get() == null) return null;

        LinkedList<UIPanelAPI> panelList = new LinkedList<>();
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI.get(), "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object modDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getModDisplay");
            panelList.add((UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(modDisplay, "getMods"));

            // The screen for adding hull mods has a different mod display object for some reason
            List<?> coreChildren = (List<?>) ReflectionUtils.invokeMethod(coreUI.get(), "getChildrenNonCopy");
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
            e.printStackTrace();
            return null;
        }
    }

    public UIPanelAPI getModsPanel() {
        return getAllModsPanels().getLast();
    }

    void modifyBuildInButton() {
        if (coreUI.get() == null) return;

        // Modify the "build-in" button
        try {
            LinkedList<UIPanelAPI> modsPanels = getAllModsPanels();
            Object modsPanel = modsPanels.getLast();
            final ButtonAPI addButton = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(modsPanel, "getAdd");
            final ButtonAPI permButton = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(modsPanel, "getPerm");

            // Modify every button that isn't the mastery or build-in buttons to trigger a refresh on click
            // This should just be the "go back" and "add" buttons
            List<?> children = (List<?>) ReflectionUtils.invokeMethod(modsPanel, "getChildrenNonCopy");
            for (Object child : children) {
                if (child instanceof ButtonAPI && child != permButton && !MASTERY_BUTTON_TAG.equals(
                        ((ButtonAPI) child).getCustomData())) {
                    final ButtonAPI button = (ButtonAPI) child;
                    final Object origListener = ReflectionUtils.getButtonListener(button);
                    if (!Proxy.isProxyClass(origListener.getClass())) {
                        ReflectionUtils.setButtonListener(button, new ActionListener() {
                            @Override
                            public void trigger(Object... args) {
                                DeferredActionPlugin.performLater(new Action() {
                                    @Override
                                    public void perform() {
                                        injectRefitScreen(false, button == addButton);
                                    }
                                }, epsilonTime);
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
                    ReflectionUtils.invokeMethodNoCatch(perm, "setOpacity", 0f);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void updateMasteryButton(boolean shouldHide) {
        if (coreUI.get() == null) return;

        UIPanelAPI modsPanel = getModsPanel();

        // Remove existing CustomPanelAPIs, which would be mastery buttons we added previously
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(modsPanel, "getChildrenNonCopy");
        Iterator<?> itr = children.listIterator();
        while (itr.hasNext()) {
            Object child = itr.next();
            if (child == masteryButtonPanelRef.get()) {
                itr.remove();
            }
        }

        if (shouldHide) return;

        ButtonAPI permButton = (ButtonAPI) ReflectionUtils.invokeMethod(modsPanel, "getPerm");
        Pair<ButtonAPI, CustomPanelAPI> masteryButtonPair = ReflectionUtils.makeButton(
                Strings.MASTERY_BUTTON_STR, new MasteryButtonPressed(this),
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM,
                permButton.getPosition().getWidth(), permButton.getPosition().getHeight(), Keyboard.KEY_Q);
        ButtonAPI masteryButton = masteryButtonPair.one;
        masteryButton.setCustomData(MASTERY_BUTTON_TAG);
        UIPanelAPI masteryButtonPanel = masteryButtonPair.two;

        modsPanel.addComponent(masteryButtonPanel).belowMid(permButton, -25f).setXAlignOffset(-5f);
        masteryButtonPanelRef = new WeakReference<>(masteryButtonPanel);
    }

    void addMPDisplay() {
        if (coreUI.get() == null) return;

        UIPanelAPI currentTab = (UIPanelAPI) ReflectionUtils.invokeMethod(coreUI.get(), "getCurrentTab");
        // Add MP display to the ship displays on the left side of the refit screen
        try {
            //noinspection unchecked
            Map<ButtonAPI, FleetMember> buttonToMemberMap = (Map<ButtonAPI, FleetMember>) ReflectionUtils.invokeMethodNoCatch(
                    currentTab, "getButtonToMember");
            if (buttonToMemberMap != null && !buttonToMemberMap.isEmpty()) {
                // Find a random button to get the sorted button list
                // However, if that button is for selecting a module, it's a different type of button
                // So, skip those with fleet member name == null
                Map.Entry<ButtonAPI, FleetMember> randomEntry = null;
                for (Map.Entry<ButtonAPI, FleetMember> entry : buttonToMemberMap.entrySet()) {
                    randomEntry = entry;
                    if (randomEntry.getValue() != null && randomEntry.getValue().getShipName() != null) {
                        break;
                    }
                }
                ButtonAPI randomButton = randomEntry.getKey();
                UIPanelAPI parent = (UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(randomButton, "getParent");
                List<?> sortedButtonList = (List<?>) ReflectionUtils.invokeMethodNoCatch(parent, "getChildrenNonCopy");
                // Delete MP panels we may have previously added
                Iterator<?> itr = sortedButtonList.listIterator();
                while (itr.hasNext()) {
                    Object child = itr.next();
                    if (child == mpPanelRef.get()) {
                        itr.remove();
                    }
                }
                float w = parent.getPosition().getWidth(), h = parent.getPosition().getHeight();
                CustomPanelAPI custom = Global.getSettings().createCustom(w, h, null);

                TooltipMakerAPI tooltipMaker = custom.createUIElement(w, h, false);
                for (int i = 0; i < sortedButtonList.size(); i++) {
                    // Should be all buttons, but the item we add isn't a button so technically the list can contain non-buttons...
                    if (!(sortedButtonList.get(i) instanceof ButtonAPI)) continue;
                    float h2 = tooltipMaker.getHeightSoFar();
                    //noinspection SuspiciousMethodCalls
                    FleetMember fm = buttonToMemberMap.get(sortedButtonList.get(i));
                    if (fm != null) {
                        ShipHullSpecAPI spec = fm.getHullSpec();
                        int currentMastery = ShipMastery.getMasteryLevel(spec);
                        int maxMastery = ShipMastery.getMaxMastery(spec);
                        if (currentMastery < maxMastery) {
                            tooltipMaker.addPara(Strings.MASTERY_LABEL_STR + String.format("%s/%s", currentMastery, maxMastery), Settings.MASTERY_COLOR, 10f).setAlignment(Alignment.LMID);
                        }
                        int mp = (int) ShipMastery.getMasteryPoints(spec);
                        if (mp > 0) {
                            tooltipMaker.addPara(mp + " MP", Settings.MASTERY_COLOR, 0f).setAlignment(Alignment.LMID);
                        }
                    }
                    float hDiff = tooltipMaker.getHeightSoFar() - h2;
                    ButtonAPI button = (ButtonAPI) sortedButtonList.get(i);
                    if (i < sortedButtonList.size() - 1 && sortedButtonList.get(i + 1) instanceof ButtonAPI) {
                        ButtonAPI nextButton = (ButtonAPI) sortedButtonList.get(i + 1);
                        tooltipMaker.addSpacer(button.getPosition().getY() - nextButton.getPosition().getY() - hDiff);
                    }
                }
                custom.addUIElement(tooltipMaker);
                parent.addComponent(custom).inBL(2f, -6f);
                mpPanelRef = new WeakReference<>((UIPanelAPI) custom);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void injectRefitScreen(boolean variantChanged) {
        injectRefitScreen(variantChanged, false);
    }

    public void injectRefitScreen(boolean variantChanged, boolean hideMasteryButton) {
        coreUI = new WeakReference<>((CoreUIAPI) ReflectionUtils.getCoreUI());

        modifyBuildInButton();
        updateMasteryButton(hideMasteryButton);
        addMPDisplay();

        syncRefitScreenWithVariant(variantChanged);
    }

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        if (CoreUITabId.REFIT.equals(id)) {
            DeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    injectRefitScreen(false);
                    checkIfRefitShipChanged();
                }
            }, epsilonTime);
            insideRefitPanel = true;
        }
    }

    /** This is called every time the active ship in the refit screen changes. */
    @Override
    public void reportAboutToRefreshCharacterStatEffects() {}


    boolean skipRefresh = false;
    /** This is called every time the active ship in the refit screen changes. */
    @Override
    public void reportRefreshedCharacterStatEffects() {
        // checkIfRefitShipChanged will call syncRefitScreenWithVariant,
        // which internally refreshes the character stats, so use a flag
        // to prevent recursion.
        if (insideRefitPanel && !skipRefresh) {
            checkIfRefitShipChanged();
        }
    }

    void checkIfRefitShipChanged() {
        coreUI = new WeakReference<>((CoreUIAPI) ReflectionUtils.getCoreUI());
        Pair<ShipAPI, ShipAPI> moduleAndRoot = getSelectedShip();
        ShipAPI module = moduleAndRoot.one;
        ShipAPI root = moduleAndRoot.two;
        ShipInfo newShipInfo = new ShipInfo(module, root);

        boolean shouldSync = false;
        if (module != null && !skipRefresh) {
            // bypass the arbitrary checks in removeMod since we're adding it back anyway
            module.getVariant().getHullMods().remove("sms_masteryHandler");
            module.getVariant().getHullMods().add("sms_masteryHandler");
            shouldSync = true;
        }

        if (!Objects.equals(currentShipInfo, newShipInfo)) {
            System.out.println(
                    "Refit ship changed: " + (
                            (currentShipInfo.rootSpec == null ? "null" : currentShipInfo.rootSpec.getHullId()) + ", " + (currentShipInfo.moduleVariant == null ? "null" :currentShipInfo.moduleVariant.getHullVariantId())) + " -> " +
                            (newShipInfo.rootSpec == null ? "null" : newShipInfo.rootSpec.getHullId()) + ", " + (newShipInfo.moduleVariant == null ? "null" :newShipInfo.moduleVariant.getHullVariantId()));
            onRefitScreenShipChanged(newShipInfo);
            currentShipInfo = newShipInfo;
        }
        if (shouldSync) {
            skipRefresh = true;
            syncRefitScreenWithVariant(false);
        }
        skipRefresh = false;
    }

    List<EffectActivationRecord> effectsToDeactivate = new ArrayList<>();
    public void onRefitScreenShipChanged(ShipInfo newInfo) {
        final ShipHullSpecAPI newSpec = newInfo.rootSpec;
        final ShipVariantAPI newVariant = newInfo.moduleVariant;

        for (int i = effectsToDeactivate.size() - 1; i >= 0; i--) {
            EffectActivationRecord toDeactivate = effectsToDeactivate.get(i);
            toDeactivate.effect.onEndRefit(toDeactivate.moduleVariant, toDeactivate.isModule);
            //System.out.println("Unapply: " + toDeactivate.id + " to " + toDeactivate.moduleVariant.getHullVariantId() + ", " + toDeactivate.isModule);
        }

        effectsToDeactivate.clear();

        if (newSpec != null && newVariant != null) {
            MasteryUtils.applyMasteryEffects(
                newSpec, newInfo.activeMasteries, false, new MasteryUtils.MasteryAction() {
                    @Override
                    public void perform(MasteryEffect effect) {
                        boolean isModule = !Objects.equals(Utils.getRestoredHullSpecId(newVariant.getHullSpec()), newSpec.getHullId());
                        if (!isModule || !effect.hasTag(MasteryTags.DOESNT_AFFECT_MODULES)) {
                            effect.onBeginRefit(newVariant, isModule);
                            //System.out.println("Apply: " + id + " to " + newVariant.getHullVariantId() + ", " + isModule);
                            effectsToDeactivate.add(new EffectActivationRecord(effect, newVariant, isModule));
                        }
                    }
                });
        }

        //System.out.println("-------------");
    }

    static class EffectActivationRecord {
        MasteryEffect effect;
        ShipVariantAPI moduleVariant;
        boolean isModule;

        EffectActivationRecord(MasteryEffect effect, ShipVariantAPI moduleVariant, boolean isModule) {
            this.effect = effect;
            this.moduleVariant = moduleVariant;
            this.isModule = isModule;
        }
    }

    static class ShipInfo {
        /** Hull spec of the root ship (so parent if ship is a module) */
        ShipHullSpecAPI rootSpec;

        /** Currently selected module variant */
        ShipVariantAPI moduleVariant;

        /** Active masteries at the time of selection -- if these change, need to refresh the ship */
        NavigableMap<Integer, Boolean> activeMasteries;

        ShipInfo(ShipAPI moduleShip, ShipAPI rootShip) {
            if (rootShip == null) {
                rootSpec = null;
                activeMasteries = new TreeMap<>();
            }
            else {
                rootSpec = Utils.getRestoredHullSpec(rootShip.getHullSpec());
                activeMasteries = ShipMastery.getActiveMasteriesCopy(rootSpec);
            }
            moduleVariant = moduleShip == null ? null : moduleShip.getVariant();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ShipInfo)) return false;
            ShipInfo o = (ShipInfo) other;
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
