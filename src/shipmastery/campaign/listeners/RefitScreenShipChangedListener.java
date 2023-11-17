package shipmastery.campaign.listeners;

import org.jetbrains.annotations.Nullable;

@Deprecated
public interface RefitScreenShipChangedListener {
    /** Used to set and reset global settings for use during refit only.
     *  Called if the ship selected for refit changes or if its masteries change.  */
    void onRefitScreenBeforeMasteriesChanged(@Nullable String oldHullSpecId, @Nullable String newHullSpecId);

    /** Used to set and reset global settings for use during refit only.
     *  Called if the ship selected for refit changes or if its masteries change. */
    void onRefitScreenAfterMasteriesChanged(@Nullable String oldHullSpecId, @Nullable String newHullSpecId);
}
