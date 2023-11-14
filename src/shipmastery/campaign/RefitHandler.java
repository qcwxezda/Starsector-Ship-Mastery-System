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
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.coreui.refit.ModPickerDialogV3;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.ui.listeners.ActionListener;
import shipmastery.ui.listeners.MasteryButtonPressed;
import shipmastery.util.ClassRefs;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.*;

public class RefitHandler implements CoreUITabListener, EveryFrameScript, CharacterStatsRefreshListener {
    boolean isFirstFrame = true;
    CoreUIAPI coreUI = null;
    float epsilonTime = 0.00001f;
    boolean insideRefitPanel = false;
    static final String MASTERY_BUTTON_TAG = "sms_mastery_button_tag";

    // Keep track of added panels to remove them in later inject calls
    UIPanelAPI mpPanelRef, masteryButtonPanelRef;

    // Keep track of the current ship's hull spec and active mastery set in the refit panel
    // Use this info to determine when to call applyEffectsOnBeginRefit and unapplyEffectsOnBeginRefit
    HullSpecAndMasteries currentHullSpecAndMasteries = null;

    static class HullSpecAndMasteries {
        String specId;

        TreeSet<Integer> activeMasteries;

        HullSpecAndMasteries(ShipHullSpecAPI spec) {
            this.specId = Utils.getBaseHullId(spec);
            activeMasteries = new TreeSet<>(MasteryUtils.getActiveMasteries(spec));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof HullSpecAndMasteries)) return false;
            HullSpecAndMasteries o = (HullSpecAndMasteries) other;
            return Objects.equals(specId, o.specId) && Objects.equals(activeMasteries, o.activeMasteries);
        }

        @Override
        public int hashCode() {
            return specId.hashCode() + activeMasteries.hashCode();
        }
    }

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
                    ClassRefs.findAllClasses();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            isFirstFrame = false;
            return;
        }

        if (!ClassRefs.foundAllClasses()) {
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
            if (currentHullSpecAndMasteries != null) {
                onRefitScreenShipChanged(currentHullSpecAndMasteries, null);
            }
            currentHullSpecAndMasteries = null;
        }
    }

    public ShipAPI getSelectedShip() {
        ShipAPI ship;
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI, "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object shipDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getShipDisplay");
            ship = (ShipAPI) ReflectionUtils.invokeMethodNoCatch(shipDisplay, "getShip");
        } catch (Exception e) {
            ship = null;
        }
        return ship;
    }

    void syncRefitScreenWithVariant() {
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI, "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            // save the variant so can't undo s-mods
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "saveCurrentVariant");
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "syncWithCurrentVariant");

            // This is necessary if allowing mastery panel to be open while in the menu for adding hullmods
            // ((ModWidget) getModsPanel()).syncWithCurrentVariant((HullVariantSpec) getSelectedShip().getVariant());

            // To disable the undo button
            ReflectionUtils.invokeMethodNoCatch(refitPanel, "setEditedSinceSave", false);
        } catch (Exception ignore) {
        }
    }

    /**
     * Multiple mods panels can appear in two ways:
     * - If adding hullmods, a separate mods panel is generated (different from getModDisplay.getMods)
     * - In rare instances, rapid clicking of the Add button can generate multiple separate mods panels
     */
    public LinkedList<UIPanelAPI> getAllModsPanels() {
        if (coreUI == null) return null;

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
                        if (subChild.getClass().equals(modDisplay.getClass())) {
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
        if (coreUI == null) return;

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
        if (coreUI == null) return;

        UIPanelAPI modsPanel = getModsPanel();

        // Remove existing CustomPanelAPIs, which would be mastery buttons we added previously
        List<?> children = (List<?>) ReflectionUtils.invokeMethod(modsPanel, "getChildrenNonCopy");
        Iterator<?> itr = children.listIterator();
        while (itr.hasNext()) {
            Object child = itr.next();
            if (child == masteryButtonPanelRef) {
                itr.remove();
            }
        }

        if (shouldHide) return;

        ButtonAPI permButton = (ButtonAPI) ReflectionUtils.invokeMethod(modsPanel, "getPerm");
        Pair<ButtonAPI, CustomPanelAPI> masteryButtonPair = ReflectionUtils.makeButton(
                Utils.getString("sms_refitScreen", "masteryButton"), new MasteryButtonPressed(this),
                Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.BOTTOM,
                permButton.getPosition().getWidth(), permButton.getPosition().getHeight(), 16);
        ButtonAPI masteryButton = masteryButtonPair.one;
        masteryButton.setCustomData(MASTERY_BUTTON_TAG);
        UIPanelAPI masteryButtonPanel = masteryButtonPair.two;

        if (!ReflectionUtils.isInRestorableMarket(coreUI)) {
            masteryButton.setEnabled(false);
        }

        modsPanel.addComponent(masteryButtonPanel).belowMid(permButton, -25f).setXAlignOffset(-5f);
        masteryButtonPanelRef = masteryButtonPanel;
    }

    void addMPDisplay() {
        if (coreUI == null) return;

        UIPanelAPI currentTab = (UIPanelAPI) ReflectionUtils.invokeMethod(coreUI, "getCurrentTab");
        // Add MP display to the ship displays on the left side of the refit screen
        try {
            //noinspection unchecked
            Map<ButtonAPI, FleetMember> buttonToMemberMap = (Map<ButtonAPI, FleetMember>) ReflectionUtils.invokeMethodNoCatch(
                    currentTab, "getButtonToMember");
            if (buttonToMemberMap != null && !buttonToMemberMap.isEmpty()) {
                ButtonAPI randomButton = buttonToMemberMap.keySet().iterator().next();
                UIPanelAPI parent = (UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(randomButton, "getParent");
                List<?> sortedButtonList = (List<?>) ReflectionUtils.invokeMethodNoCatch(parent, "getChildrenNonCopy");
                // Delete MP panels we may have previously added
                Iterator<?> itr = sortedButtonList.listIterator();
                while (itr.hasNext()) {
                    Object child = itr.next();
                    if (child == mpPanelRef) {
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
                    //noinspection ReassignedVariable,SuspiciousMethodCalls
                    FleetMember fm = buttonToMemberMap.get(sortedButtonList.get(i));
                    if (fm != null) {
                        ShipHullSpecAPI spec = fm.getHullSpec();
                        int currentMastery = MasteryUtils.getMasteryLevel(spec);
                        int maxMastery = MasteryUtils.getMaxMastery(spec);
                        if (currentMastery < maxMastery) {
                            tooltipMaker.addPara(Utils.getString("sms_refitScreen", "masteryLabel") + String.format("%s/%s", currentMastery, maxMastery), Settings.MASTERY_COLOR, 10f).setAlignment(Alignment.LMID);
                        }
                        int mp = (int) MasteryUtils.getMasteryPoints(spec);
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
                mpPanelRef = custom;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void injectRefitScreen(boolean variantChanged) {
        injectRefitScreen(variantChanged, false);
    }

    public void injectRefitScreen(boolean variantChanged, boolean hideMasteryButton) {
        coreUI = (CoreUIAPI) ReflectionUtils.getCoreUI();
        checkIfRefitShipChanged();

        modifyBuildInButton();
        updateMasteryButton(hideMasteryButton);
        addMPDisplay();

        if (variantChanged) {
            syncRefitScreenWithVariant();
        }
    }

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        if (CoreUITabId.REFIT.equals(id)) {
            DeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    injectRefitScreen(false);
                }
            }, epsilonTime);
            insideRefitPanel = true;
        }
    }

    @Override
    public void reportAboutToRefreshCharacterStatEffects() {}

    /** This is called every time the active ship in the refit screen changes. */
    @Override
    public void reportRefreshedCharacterStatEffects() {
        DeferredActionPlugin.performLater(new Action() {
            @Override
            public void perform() {
                checkIfRefitShipChanged();
            }
        }, epsilonTime);
    }

    void checkIfRefitShipChanged() {
        if (!insideRefitPanel) return;
        coreUI = (CoreUIAPI) ReflectionUtils.getCoreUI();
        ShipAPI ship = getSelectedShip();
        HullSpecAndMasteries newSpec = ship == null ? null : new HullSpecAndMasteries(ship.getHullSpec());

        if (!Objects.equals(currentHullSpecAndMasteries, newSpec)) {
            onRefitScreenShipChanged(
                    currentHullSpecAndMasteries,
                    newSpec);
            currentHullSpecAndMasteries = newSpec;
        }
    }

    public void onRefitScreenShipChanged(HullSpecAndMasteries oldSpec, HullSpecAndMasteries newSpec) {
        if (oldSpec != null && oldSpec.specId != null) {
            for (int i : oldSpec.activeMasteries.descendingSet()) {
                MasteryEffect effect = MasteryUtils.getMasteryEffect(oldSpec.specId, i);
                effect.unapplyEffectsOnEndRefit(Global.getSettings().getHullSpec(oldSpec.specId), MasteryUtils.makeEffectId(effect, i));
            }
        }

        if (newSpec != null && newSpec.specId != null) {
            for (int i : newSpec.activeMasteries) {
                MasteryEffect effect = MasteryUtils.getMasteryEffect(newSpec.specId, i);
                effect.applyEffectsOnBeginRefit(Global.getSettings().getHullSpec(newSpec.specId), MasteryUtils.makeEffectId(effect, i));
            }
        }
    }
}
