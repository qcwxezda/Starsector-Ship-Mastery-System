package shipmastery.util;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.ui.impl.StandardTooltipV2;
import org.apache.log4j.Logger;
import shipmastery.plugin.ModPlugin;
import shipmastery.ui.triggers.ActionListener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;


public class FleetMemberTooltipCreator {
    public static Method setTooltipMethod;
    public static Field rendererFleetMemberField;
    public static final Logger logger = Logger.getLogger(FleetMemberTooltipCreator.class);
    public static MethodHandle modifyShipButtons;
    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    static {
        try {
            Class<?> tooltipCreatorClass =
                    ModPlugin.classLoader.loadClass("shipmastery.util.FleetMemberTooltipCreator");
            modifyShipButtons = lookup.findStatic(
                    tooltipCreatorClass,
                    "modifyShipButtons",
                    MethodType.methodType(void.class, TooltipMakerAPI.class, Object.class, OnShipButtonClicked.class));
        }
        catch  (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @SuppressWarnings("unused")
    public static void modifyShipButtons(
            TooltipMakerAPI tooltip,
            Object shipListUI,
            OnShipButtonClicked onClick) {
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
                        onClick.onClicked(member, args);
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
