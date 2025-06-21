package shipmastery.campaign.listeners;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

public interface FleetSyncListener {
    void onFleetSync(CampaignFleetAPI fleet);
}
