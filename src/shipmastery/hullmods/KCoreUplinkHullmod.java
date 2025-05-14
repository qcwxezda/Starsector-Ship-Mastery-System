package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.campaign.items.KCoreUplinkPlugin;
import shipmastery.util.Utils;

public class KCoreUplinkHullmod extends BaseHullMod implements HullModFleetEffect {
    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        KCoreUplinkPlugin.applyKCoreCRPenalty(stats, id);
    }

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (!fleet.isPlayerFleet()) return;
        for (var fm : fleet.getFleetData().getMembersListCopy()) {
            boolean handle = false;
            var id = fm.getCaptain().getAICoreId();
            if (id != null) {
                var spec = Global.getSettings().getCommoditySpec(id);
                if (spec != null && spec.hasTag(KCoreUplinkPlugin.IS_AUTOCONSTRUCT_TAG)) {
                    handle = true;
                }
            }
            if (handle) {
                Utils.addPermaModCloneVariantIfNeeded(fm, "sms_k_core_uplink_handler");
            } else {
                fm.getVariant().removePermaMod("sms_k_core_uplink_handler");
            }
        }
    }
}
