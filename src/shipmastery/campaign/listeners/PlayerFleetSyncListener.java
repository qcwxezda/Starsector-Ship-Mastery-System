package shipmastery.campaign.listeners;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public interface PlayerFleetSyncListener {
    void onFleetSync(CampaignFleetAPI fleet);
}
