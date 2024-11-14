package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.ui.triggers.DialogDismissedListener;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class ReflectionUtils {

    public static Pair<ButtonAPI, CustomPanelAPI> makeButton(String text, ActionListener handler, Color base, Color bg, float width, float height) {
        return makeButton(text, handler, base, bg, Alignment.MID, CutStyle.ALL, width, height, -1);
    }

    public static Pair<ButtonAPI, CustomPanelAPI> makeButton(String text, ActionListener handler, Color base, Color bg, Alignment align, CutStyle style, float width, float height, int hotkey) {
        CustomPanelAPI container = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI ttm = container.createUIElement(width, height, false);
        ttm.setButtonFontOrbitron20();
        ButtonAPI button = ttm.addButton(text, null, base, bg, align, style, width, height, 0f);
        setButtonListener(button, handler);
        if (hotkey >= 0) {
            button.setShortcut(hotkey, true);
        }
        container.addUIElement(ttm);
        return new Pair<>(button, container);
    }

    public static Object getField(Object o, String fieldName) {
        return getFieldWithClass(o.getClass(), o, fieldName);
    }

    public static Object getFieldNoCatch(Object o, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        return getFieldWithClassNoCatch(o.getClass(), o, fieldName);
    }

    public static Object getFieldWithClass(Class<?> cls, Object o, String fieldName) {
        try {
            return getFieldWithClassNoCatch(cls, o, fieldName);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getFieldWithClassNoCatch(Class<?> cls, Object o, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        if (o == null) return null;
        Field field = cls.getDeclaredField(fieldName);
        field.setAccessible(true);

        return field.get(o);
    }

    public static void setField(Object o, String fieldName, Object value) {
        if (o == null) return;
        try {
            Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(o, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static UIPanelAPI getCoreUI() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();

        CoreUIAPI core;
        if (dialog == null) {
            core = (CoreUIAPI) ReflectionUtils.getField(campaignUI, "core");
        }
        else {
            core = (CoreUIAPI) ReflectionUtils.invokeMethod(dialog, "getCoreUI");
        }
        return core == null ? null : (UIPanelAPI) core;
    }

    public static Object invokeMethod(Object o, String methodName, Object... args) {
        return invokeMethodExt(o, methodName, false, args);
    }

    public static Object invokeMethodExt(Object o, String methodName, boolean isDeclaredAndHidden, Object... args) {
        try {
            return invokeMethodNoCatchExt(o, methodName, isDeclaredAndHidden, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeMethodExtWithClasses(Object o, String methodName, boolean isDeclaredAndHidden, Class<?>[] classes, Object... args) {
        try {
            return invokeMethodNoCatchExtWithClasses(o, methodName, isDeclaredAndHidden, classes, args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object invokeMethodNoCatch(Object o, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return invokeMethodNoCatchExt(o, methodName, false, args);
    }

    public static Object invokeMethodNoCatchExt(Object o, String methodName, boolean isDeclaredAndHidden, Object... args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        if (o == null) return null;
        Class<?>[] argClasses = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            argClasses[i] = args[i].getClass();
            // unbox
            if (argClasses[i] == Integer.class) {
                argClasses[i] = int.class;
            } else if (argClasses[i] == Boolean.class) {
                argClasses[i] = boolean.class;
            } else if (argClasses[i] == Float.class) {
                argClasses[i] = float.class;
            }
        }
        return invokeMethodNoCatchExtWithClasses(o, methodName, isDeclaredAndHidden, argClasses, args);
    }

    public static Object invokeMethodNoCatchExtWithClasses(Object o, String methodName, boolean isDeclaredAndHidden, Class<?>[] classes, Object[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = isDeclaredAndHidden ? o.getClass().getDeclaredMethod(methodName, classes) : o.getClass().getMethod(methodName, classes);
        if (isDeclaredAndHidden) {
            method.setAccessible(true);
        }
        return method.invoke(o, args);
    }

    public static void setButtonListener(ButtonAPI button, ActionListener listener) {
        invokeMethodExtWithClasses(button, "setListener", false, new Class[]{ClassRefs.actionListenerInterface},
                                   listener.getProxy());
    }

    public static Object getButtonListener(ButtonAPI button) {
        return invokeMethod(button, "getListener");
    }

    public static GenericDialogData showGenericDialog(
            String text,
            String confirmText,
            @Nullable String cancelText,
            float width,
            float height,
            @Nullable DialogDismissedListener listener) {
        try {
            Constructor<?> cons = ClassRefs.confirmDialogClass
                    .getConstructor(
                            float.class,
                            float.class,
                            ClassRefs.uiPanelClass,
                            ClassRefs.dialogDismissedInterface,
                            String.class,
                            String[].class);
            Object confirmDialog = cons.newInstance(
                    width,
                    height,
                    getField(Global.getSector().getCampaignUI(), "screenPanel"),
                    listener == null ? null : listener.getProxy(),
                    text,
                    cancelText == null ? new String[]{confirmText} : new String[] {confirmText, cancelText}
            );
            Method show = confirmDialog.getClass().getMethod("show", float.class, float.class);
            show.invoke(confirmDialog, 0.25f, 0.25f);
            LabelAPI label = (LabelAPI) invokeMethod(confirmDialog, "getLabel");
            ButtonAPI confirmButton = (ButtonAPI) invokeMethod(confirmDialog, "getButton", 0);
            // ButtonAPI cancelButton = (ButtonAPI) invokeMethod(confirmDialog, "getButton", 1);
            confirmButton.setShortcut(Keyboard.KEY_G, true);
            return new GenericDialogData(
                    label,
                    (UIPanelAPI) invokeMethod(confirmDialog, "getInnerPanel"),
                    (UIPanelAPI) confirmDialog);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class GenericDialogData {
        public final LabelAPI textLabel;
        public final UIPanelAPI panel;
        public final UIPanelAPI dialog;

        public GenericDialogData(LabelAPI label, UIPanelAPI panel, UIPanelAPI dialog) {
            textLabel = label;
            this.panel = panel;
            this.dialog = dialog;
        }
    }

    /** Mimics the conditions for a ship to be able to be restored */
    public static boolean isInRestorableMarket(UIPanelAPI coreUI) {
        try {
            Object currentTab = ReflectionUtils.invokeMethodNoCatch(coreUI, "getCurrentTab");
            Object refitPanel = ReflectionUtils.invokeMethodNoCatch(currentTab, "getRefitPanel");
            Object refitTab = ReflectionUtils.invokeMethodNoCatch(refitPanel, "getRefitTab");
            SectorEntityToken other = (SectorEntityToken) ReflectionUtils.invokeMethodNoCatch(refitTab, "getOther");
            Object tradeMode = ReflectionUtils.invokeMethodNoCatch(coreUI, "getTradeMode");
            return tradeMode == CampaignUIAPI.CoreUITradeMode.OPEN && other != null && other.getMarket() != null && !other.getMarket().isPlanetConditionMarketOnly();
        } catch (Exception e) {
            return false;
        }
    }
}
