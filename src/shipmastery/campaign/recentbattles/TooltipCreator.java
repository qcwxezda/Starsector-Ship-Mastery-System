package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import org.apache.log4j.Logger;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;


public class TooltipCreator {
    public static Method setTooltipMethod;
    public static Field rendererFleetMemberField;
    public static Logger logger = Logger.getLogger(TooltipCreator.class);

    public static void saveScrollbarLocation(RecentBattlesIntel intel, TooltipMakerAPI tooltip) {
        try {
            // Container's scrollbar location should be more accurate, won't cause jumps
            // when refreshing while scrolling
            Object contentContainer = ReflectionUtils.invokeMethodNoCatch(tooltip.getExternalScroller(),
                                                                          "getContentContainer");
            float yOffset = (float) ReflectionUtils.invokeMethod(contentContainer, "getYOffset");
            intel.saveScrollbarLocation(yOffset);
        }
        catch (Exception e) {
            intel.saveScrollbarLocation(tooltip.getExternalScroller().getYOffset());
        }
    }

    @SuppressWarnings("unused")
    public static void modifyShipButtons(
            final RecentBattlesIntel intel,
            final IntelUIAPI intelUI,
            final TooltipMakerAPI tooltip,
            Object shipListUI) {
        try {
            Object listUI = ReflectionUtils.invokeMethodNoCatch(shipListUI, "getList");
            List<?> buttonsList = (List<?>) ReflectionUtils.invokeMethodNoCatch(listUI, "getItems");
            for (Object o : buttonsList) {
                ButtonAPI button = (ButtonAPI) o;
                button.setButtonPressedSound("ui_button_pressed");

                Object renderer = ReflectionUtils.invokeMethodNoCatch(button, "getRenderer");
                if (rendererFleetMemberField == null) {
                    for (Field field : renderer.getClass().getDeclaredFields()) {
                        if (FleetMember.class.isAssignableFrom(field.getType())) {
                            rendererFleetMemberField = field;
                            field.setAccessible(true);
                            break;
                        }
                    }
                }

                // Treat the possible NPE like any other exception
                //noinspection DataFlowIssue
                final FleetMember member = (FleetMember) rendererFleetMemberField.get(renderer);
                ReflectionUtils.setButtonListener(button, new ActionListener() {
                    @Override
                    public void trigger(Object... args) {
                        saveScrollbarLocation(intel, tooltip);
                        intel.selectFleetMember(member);
                        intelUI.updateUIForItem(intel);
                    }
                });

                if (setTooltipMethod == null) {
                    for (Method method : button.getClass().getMethods()) {
                        if ("setTooltip".equals(method.getName())) {
                            setTooltipMethod = method;
                            break;
                        }
                    }
                }

                if (setTooltipMethod != null) {
                    setTooltipMethod.invoke(
                            button,  0f, StandardTooltipV2.createFleetMemberTooltipPreDeploy(
                                    member,
                                    member.getCaptain().getStats()));
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed to modify ship buttons", e);
        }
    }
}
