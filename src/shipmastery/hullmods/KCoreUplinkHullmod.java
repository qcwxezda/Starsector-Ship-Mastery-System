package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.BaseKCorePlugin;
import shipmastery.campaign.items.KCoreUplinkPlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class KCoreUplinkHullmod extends BaseHullMod implements HullModFleetEffect {

    public static KCoreUplinkPlugin.KCoreUplinkData savedPenaltyData = null;

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var fm = stats.getFleetMember();
        if (fm == null || fm.getCaptain() == null) return;
        if (Misc.isAutomated(fm)) return;
        if (fm.getFleetCommander() == null || !fm.getFleetCommander().isPlayer()) return;

        var captain = fm.getCaptain();
        if (!captain.isAICore()) return;
        var core = Global.getSettings().getCommoditySpec(captain.getAICoreId());
        if (core == null || !core.hasTag(BaseKCorePlugin.IS_K_CORE_TAG)) return;

        float penalty = savedPenaltyData == null ? 0f : savedPenaltyData.crPenalty();
        if (penalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Items.uplinkPenaltyDesc);
        }
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
        savedPenaltyData = KCoreUplinkPlugin.getKCoreCRPointsAndPenalty();
        for (var fm : fleet.getFleetData().getMembersListCopy()) {
            boolean handle = false;
            var id = fm.getCaptain().getAICoreId();
            if (id != null) {
                var spec = Global.getSettings().getCommoditySpec(id);
                if (spec != null && spec.hasTag(BaseKCorePlugin.IS_K_CORE_TAG)) {
                    handle = true;
                }
            }
            if (handle) {
                Utils.addPermaModCloneVariantIfNeeded(fm, "sms_k_core_uplink_handler", false);
            } else {
                fm.getVariant().removePermaMod("sms_k_core_uplink_handler");
            }
        }
    }
}
