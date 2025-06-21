package shipmastery.campaign.listeners;

import com.fs.starfarer.api.campaign.CoreUITabId;

public interface CoreTabListener {
    void onCoreTabOpened(CoreUITabId id);
    void onCoreUIDismissed();
}
