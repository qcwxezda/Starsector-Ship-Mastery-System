package shipmastery.campaign.listeners;

import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CoreUITabListener;
import com.fs.starfarer.api.ui.UIPanelAPI;
import shipmastery.campaign.CoreInteractionListenerExt;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.List;

public class CoreTabListenerHandler implements CoreUITabListener {
    private final List<CoreTabListener> listeners = new ArrayList<>();

    @Override
    public void reportAboutToOpenCoreTab(CoreUITabId id, Object param) {
        DeferredActionPlugin.performLater(() -> {
            UIPanelAPI core = ReflectionUtils.getCoreUI();
            final CoreInteractionListener origListener = (CoreInteractionListener) ReflectionUtils.invokeMethod(core, "getListener");
            for (var listener : listeners) {
                listener.onCoreTabOpened(id);
            }

            if (!(origListener instanceof CoreInteractionListenerExt)) {
                ReflectionUtils.invokeMethodExtWithClasses(
                        core,
                        "setListener",
                        false,
                        new Class<?>[]{CoreInteractionListener.class},
                        (CoreInteractionListenerExt) () -> {
                            if (origListener != null) {
                                origListener.coreUIDismissed();
                            }
                            for (var listener : listeners) {
                                listener.onCoreUIDismissed();
                            }
                        });
            }
        }, 0f);
    }

    @SuppressWarnings("unused")
    public void registerListener(CoreTabListener listener) {
        listeners.add(listener);
    }
}
