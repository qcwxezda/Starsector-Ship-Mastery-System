package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.campaign.items.BaseKCorePlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class CuratorPlayerHullmod extends CuratorNPCHullmod implements HullModFleetEffect {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        // Don't apply crew modification
    }

    @Override
    protected boolean isNPCVersion() {
        return false;
    }

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

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
        boolean active = Global.getSector().getMemoryWithoutUpdate().getBoolean(Strings.Campaign.K_CORE_AMP_INTEGRATED);
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            // Remove the NPC version of the hullmod if it exists
            if (fm.getVariant().hasHullMod("sms_curator_npc_hullmod")) {
                fm.getVariant().removePermaMod("sms_curator_npc_hullmod");
            }

            boolean remove = !active;
            remove |= fm.getCaptain() == null || !fm.getCaptain().isAICore() || fm.getCaptain().getAICoreId() == null;

            if (!remove) {
                var spec = Global.getSettings().getCommoditySpec(fm.getCaptain().getAICoreId());
                remove = spec == null || !spec.hasTag(BaseKCorePlugin.IS_K_CORE_TAG);
            }

            if (remove) {
                if (fm.getVariant().hasHullMod("sms_curator_player_hullmod")) {
                    fm.getVariant().removePermaMod("sms_curator_player_hullmod");
                }
            } else {
                if (!fm.getVariant().hasHullMod("sms_curator_player_hullmod")) {
                    Utils.addPermaModCloneVariantIfNeeded(fm, "sms_curator_player_hullmod");
                }
            }
        }
    }
}
