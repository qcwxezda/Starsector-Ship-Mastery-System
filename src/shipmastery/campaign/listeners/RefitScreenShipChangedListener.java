package shipmastery.campaign.listeners;

import org.jetbrains.annotations.Nullable;

public interface RefitScreenShipChangedListener {
    /** Used to set and reset global settings for use during refit only. */
    void onRefitScreenBeforeMasteriesChanged(@Nullable String oldHullSpecId, @Nullable String newHullSpecId);

    /** Used to set and reset global settings for use during refit only. */
    void onRefitScreenAfterMasteriesChanged(@Nullable String oldHullSpecId, @Nullable String newHullSpecId);
}
