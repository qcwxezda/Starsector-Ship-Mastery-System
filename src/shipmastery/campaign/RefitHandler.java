package shipmastery.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.coreui.refit.ModPickerDialogV3;
import shipmastery.Settings;
import shipmastery.listeners.ActionListener;
import shipmastery.listeners.MasteryButtonPressed;
import shipmastery.util.ClassRefs;
import shipmastery.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

public class RefitHandler implements CoreUITabListener, EveryFrameScript {

    boolean insideRefitScreen = false;
    boolean needRefresh = true;
    boolean isFirstFrame = true;

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
            // Since the core UI's "screenPanel" isn't created on the first frame, trying to do anything with the UI
            // on the first frame will cause an NPE. Therefore, we will initialize the screenPanel before trying
            // to call findAllClasses, if it hasn't been initialized already.
            try {
                CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
                Field field = campaignUI.getClass().getDeclaredField("screenPanel");
                field.setAccessible(true);
                if (field.get(campaignUI) == null) {
                    field.set(campaignUI,
                            field.getType()
                                    .getConstructor(float.class, float.class)
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

        if (Global.getSector() == null || Global.getSector().getCampaignUI() == null) return;

        if (!insideRefitScreen || !needRefresh) return;

        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!CoreUITabId.REFIT.equals(ui.getCurrentCoreTab())) {
            insideRefitScreen = false;
            return;
        }
        // Due to a bug, if the player ESCs out of the refit screen in a market, the core tab is still shown as REFIT
        // even though it's been closed. To combat this, check if the savedOptionList is empty. If it is, we're still
        // in the refit screen; otherwise, we've ESCed out of the refit screen.
        else if (ui.getCurrentInteractionDialog() != null
                && ui.getCurrentInteractionDialog().getOptionPanel() != null
                && !ui.getCurrentInteractionDialog().getOptionPanel().getSavedOptionList().isEmpty()) {
            insideRefitScreen = false;
            return;
        }

        CoreUIAPI core = (CoreUIAPI) ReflectionUtils.getCoreUI();
        UIPanelAPI currentTab = (UIPanelAPI) ReflectionUtils.invokeMethod(core, "getCurrentTab");

        modifyBuildInButton(core, currentTab);
        addMPDisplay(currentTab);
        needRefresh = false;
    }

    private ShipAPI getSelectedShip(CoreUIAPI core) {
        ShipAPI ship;
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(core, "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object shipDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getShipDisplay");
            ship = (ShipAPI) ReflectionUtils.invokeMethodNoCatch(shipDisplay, "getShip");
        }
        catch (Exception e) {
            ship = null;
        }
        return ship;
    }

    void modifyBuildInButton(CoreUIAPI core, UIPanelAPI currentTab) {
        // Modify the "build-in" button
        try {
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object modDisplay = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getModDisplay");

            // The screen for adding hull mods has a different mod display object for some reason
            List<?> coreChildren = (List<?>) ReflectionUtils.invokeMethod(core, "getChildrenNonCopy");
            boolean isAddingHullMods = false;

            outer:
            for (Object child : coreChildren) {
                if (child instanceof ModPickerDialogV3) {
                    List<?> subChildren = (List<?>) ReflectionUtils.invokeMethod(child, "getChildrenNonCopy");
                    for (Object subChild : subChildren) {
                        if (subChild.getClass().equals(modDisplay.getClass())) {
                            modDisplay = subChild;
                            isAddingHullMods = true;
                            break outer;
                        }
                    }
                }
            }
            Object modsPanel = ReflectionUtils.invokeMethodNoCatch(modDisplay, "getMods");
            ButtonAPI addButton = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(modsPanel, "getAdd");
            ButtonAPI permButton = (ButtonAPI) ReflectionUtils.invokeMethodNoCatch(modsPanel, "getPerm");
            permButton.setText("Mastery");

            // Modify the add button so that it does what it normally does, but also triggers a refresh since the
            // "build in" button changes when adding hull mods.
            final Object origListener = ReflectionUtils.getButtonListener(addButton);
            if (!Proxy.isProxyClass(origListener.getClass())) {
                ReflectionUtils.setButtonListener(addButton, new ActionListener() {
                    @Override
                    public void trigger(Object... args) {
                        needRefresh = true;
                        ReflectionUtils.invokeMethodExtWithClasses(origListener, "actionPerformed", false, new Class[]{Object.class, Object.class}, args);
                    }
                });
            }

            if (isAddingHullMods) {
                ReflectionUtils.invokeMethodNoCatch(permButton, "setOpacity", 0f);
            }
            else {
                ReflectionUtils.setButtonListener(permButton, new MasteryButtonPressed(getSelectedShip(core)));
                permButton.setShortcut(16, true);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void addMPDisplay(UIPanelAPI currentTab) {
        // Add MP display to the ship displays on the left side of the refit screen
        try {
            //noinspection unchecked
            Map<ButtonAPI, FleetMember> buttonToMemberMap = (Map<ButtonAPI, FleetMember>) ReflectionUtils.invokeMethodNoCatch(currentTab, "getButtonToMember");
            if (buttonToMemberMap != null && !buttonToMemberMap.isEmpty()) {
                ButtonAPI randomButton = buttonToMemberMap.keySet().iterator().next();
                UIPanelAPI parent = (UIPanelAPI) ReflectionUtils.invokeMethodNoCatch(randomButton, "getParent");
                List<?> sortedButtonList = (List<?>) ReflectionUtils.invokeMethodNoCatch(parent, "getChildrenNonCopy");

                // Haven't added anything yet if the two sizes are the same
                if (sortedButtonList.size() == buttonToMemberMap.size()) {
                    float w = parent.getPosition().getWidth(), h = parent.getPosition().getHeight();
                    CustomPanelAPI custom = Global.getSettings().createCustom(w, h, null);
                    TooltipMakerAPI tooltipMaker = custom.createUIElement(w, h, false);
                    tooltipMaker.setParaOrbitronLarge();
                    for (int i = 0; i < sortedButtonList.size(); i++) {
                        float h2 = tooltipMaker.getHeightSoFar();
                        tooltipMaker.addPara("1536 MP", Settings.masteryColor, 0f).setAlignment(Alignment.LMID);
                        float hDiff = tooltipMaker.getHeightSoFar() - h2;
                        // Should be all buttons, but the item we add isn't a button so technically the list can contain non-buttons...
                        if (sortedButtonList.get(i) instanceof ButtonAPI) {
                            ButtonAPI button = (ButtonAPI) sortedButtonList.get(i);
                            if (i < sortedButtonList.size() - 1 && sortedButtonList.get(i + 1) instanceof ButtonAPI) {
                                ButtonAPI nextButton = (ButtonAPI) sortedButtonList.get(i + 1);
                                tooltipMaker.addSpacer(button.getPosition().getY() - nextButton.getPosition().getY() - hDiff);
                            }
                        }
                    }
                    tooltipMaker.setParaFontDefault();
                    custom.addUIElement(tooltipMaker);
                    parent.addComponent(custom).inBL(2f, -6f);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        if (CoreUITabId.REFIT.equals(id)) {
            insideRefitScreen = true;
            needRefresh = true;
        }
    }
}