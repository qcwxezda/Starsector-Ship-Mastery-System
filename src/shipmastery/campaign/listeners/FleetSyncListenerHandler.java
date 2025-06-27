package shipmastery.campaign.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;

import java.util.ArrayList;
import java.util.List;

public class FleetSyncListenerHandler extends BaseHullMod implements HullModFleetEffect {
    private final List<PlayerFleetSyncListener> listeners = new ArrayList<>();

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public boolean withAdvanceInCampaign() {return false;}

    @Override
    public boolean withOnFleetSync() {return true;}

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (!fleet.isPlayerFleet()) return;
        for (var listener : listeners) {
            listener.onFleetSync(fleet);
        }
    }

    public static void registerListener(PlayerFleetSyncListener listener) {
        ((FleetSyncListenerHandler) Global.getSettings().getHullModSpec("sms_fleet_sync_listener_handler").getFleetEffect()).listeners.add(listener);
    }

    public static void clearListeners() {
        ((FleetSyncListenerHandler) Global.getSettings().getHullModSpec("sms_fleet_sync_listener_handler").getFleetEffect()).listeners.clear();
    }
}
