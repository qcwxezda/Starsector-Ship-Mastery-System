package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.campaign.ui.UITable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public abstract class ClassRefs {
    /** The class that CampaignUIAPI.showConfirmDialog instantiates. We need this because showConfirmDialog doesn't work
     *  if any core UI is open. */
    public static Class<?> confirmDialogClass;
    /** Interface that contains a single method: actionPerformed */
    public static Class<?> actionListenerInterface;
    /** Interface that contains a single method: dialogDismissed */
    public static Class<?> dialogDismissedInterface;
    public static Class<?> uiPanelClass;
    public static Class<?> uiTableDelegateClass;
    public static String uiTableDelegateMethodName;
    private static boolean foundAllClasses = false;
    public static boolean foundAllClasses(){
        return foundAllClasses;
    }

    /** [witness] needs to implement the action listener interface */
    public static void findActionListenerInterface(Object witness) {
        actionListenerInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "actionPerformed");
    }

    /** [witness] needs to implement the dialog dismissed interface */
    public static void findDialogDismissedInterface(Object witness) {
        dialogDismissedInterface = findInterfaceByMethod(witness.getClass().getInterfaces(), "dialogDismissed");
    }

    public static void findUIPanelClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        try {
            Field field = campaignUI.getClass().getDeclaredField("screenPanel");
            uiPanelClass = field.getType();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void findUITableDelegateClass() {
        for (Class<?> cls : UITable.class.getDeclaredClasses()) {
            if (cls.isInterface() && cls.getDeclaredMethods().length == 1) {
                uiTableDelegateClass = cls;
                uiTableDelegateMethodName = cls.getDeclaredMethods()[0].getName();
                return;
            }
        }
    }

    public static void findConfirmDialogClass() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        // If we don't know the confirmation dialog class, try to create a confirmation dialog in order to access it
        try {
            boolean isPaused = Global.getSector().isPaused();
            if (confirmDialogClass == null && campaignUI.showConfirmDialog("", "", "", null, null)) {
                Object screenPanel = ReflectionUtils.getField(campaignUI, "screenPanel");
                List<?> children = (List<?>) ReflectionUtils.invokeMethod(screenPanel, "getChildrenNonCopy");
                // the confirm dialog will be the last child
                Object panel = children.get(children.size() - 1);
                confirmDialogClass = panel.getClass();
                // we have the class, dismiss the dialog
                Method dismiss = confirmDialogClass.getMethod("dismiss", int.class);
                dismiss.invoke(panel, 0);
                Global.getSector().setPaused(isPaused);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void findAllClasses() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (confirmDialogClass == null) {
            findConfirmDialogClass();
        }
        if (dialogDismissedInterface == null) {
            findDialogDismissedInterface(campaignUI);
        }
        if (actionListenerInterface == null) {
            findActionListenerInterface(campaignUI);
        }
        if (uiPanelClass == null) {
            findUIPanelClass();
        }
        if (uiTableDelegateClass == null) {
            findUITableDelegateClass();
        }
        if (confirmDialogClass != null && dialogDismissedInterface != null && actionListenerInterface != null && uiPanelClass != null && uiTableDelegateClass != null) {
            foundAllClasses = true;
        }
    }

    /** Tries to find an interface among [interfaces] that has [methodName] as its only method. */
    private static Class<?> findInterfaceByMethod(Class<?>[] interfaces, String methodName) {
        for (Class<?> cls : interfaces) {
            Method[] methods = cls.getDeclaredMethods();
            if (methods.length != 1) {
                continue;
            }
            Method method = methods[0];
            if (method.getName().equals(methodName)) {
                return cls;
            }
        }

        throw new RuntimeException("Interface with only method " + methodName + " not found; perhaps invalid witness used?");
    }
}
