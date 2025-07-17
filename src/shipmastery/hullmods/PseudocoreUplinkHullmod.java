package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.PseudocoreInterface;
import shipmastery.campaign.items.PseudocoreUplinkPlugin;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;
import shipmastery.util.Strings;

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
        var coreId = captain.getAICoreId();
        var plugin = PseudocoreInterface.getPluginForPseudocore(coreId);
        if (plugin == null) return;

        float penalty = savedPenaltyData == null ? 0f : savedPenaltyData.crPenalty();
        if (penalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Items.uplinkPenaltyDesc);
        }
    }

    @Override
    public void onPlayerFleetSync() {
        var fleet = Global.getSector().getPlayerFleet();
        savedPenaltyData = PseudocoreUplinkPlugin.getPseudocoreCRPointsAndPenalty();
        fleet.getFleetData().getMembersListCopy().forEach(fm -> {
            var captain = fm.getCaptain();
            if (captain == null || !captain.isAICore()) {
                fm.getVariant().removePermaMod("sms_pseudocore_uplink_handler");
            }
        });
    }
}
