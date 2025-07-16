package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.BasePseudocorePlugin;
import shipmastery.campaign.items.PseudocoreUplinkPlugin;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PseudocoreUplinkHullmod extends BaseHullMod implements PlayerFleetSyncListener {

    public static PseudocoreUplinkPlugin.PseudocoreUplinkData savedPenaltyData = null;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var fm = stats.getFleetMember();
        if (fm == null || fm.getCaptain() == null) return;
        if (Misc.isAutomated(fm)) return;
        if (fm.getFleetCommander() == null || !fm.getFleetCommander().isPlayer()) return;

        var captain = fm.getCaptain();
        if (!captain.isAICore()) return;
        var core = Global.getSettings().getCommoditySpec(captain.getAICoreId());
        if (core == null || !core.hasTag(BasePseudocorePlugin.IS_PSEUDOCORE_TAG)) return;

        float penalty = savedPenaltyData == null ? 0f : savedPenaltyData.crPenalty();
        if (penalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Items.uplinkPenaltyDesc);
        }
    }

    @Override
    public void onPlayerFleetSync() {
        var fleet = Global.getSector().getPlayerFleet();
        savedPenaltyData = PseudocoreUplinkPlugin.getPseudocoreCRPointsAndPenalty();
        for (var fm : fleet.getFleetData().getMembersListCopy()) {
            boolean handle = false;
            var id = fm.getCaptain().getAICoreId();
            if (id != null) {
                var spec = Global.getSettings().getCommoditySpec(id);
                if (spec != null && spec.hasTag(BasePseudocorePlugin.IS_PSEUDOCORE_TAG)) {
                    handle = true;
                }
            }
            if (handle) {
                Utils.addPermaModCloneVariantIfNeeded(fm, "sms_pseudocore_uplink_handler", false);
            } else {
                fm.getVariant().removePermaMod("sms_pseudocore_uplink_handler");
            }
        }
    }
}
