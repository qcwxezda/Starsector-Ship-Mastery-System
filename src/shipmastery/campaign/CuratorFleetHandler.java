package shipmastery.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetInflater;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import java.util.ArrayList;

public class CuratorFleetHandler implements FleetInflationListener {
    @Override
    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        // Curator fleets should not have crew or built-in d-mods
        if (fleet.getFaction() != null && "sms_curator".equals(fleet.getFaction().getId())) {
            for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
                fm.getVariant().addPermaMod("sms_curator_npc_hullmod", false);
                var toRemove = new ArrayList<String>();
                for (var hullmod : fm.getVariant().getHullMods()) {
                    var spec = Global.getSettings().getHullModSpec(hullmod);
                    if (spec.hasTag(Tags.HULLMOD_DMOD) && fm.getHullSpec().isBuiltInMod(hullmod)) {
                        toRemove.add(hullmod);
                    }
                }
                for (var hullmod : toRemove) {
                    fm.getVariant().addSuppressedMod(hullmod);
                }
            }
        }
    }
}
