package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.campaign.items.PseudocoreUplinkPlugin;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.CampaignUtils;
import shipmastery.util.Strings;

public class PseudocoreUplinkHullmod extends BaseHullMod implements PlayerFleetSyncListener {

    public static final String USED_UPLINK_MEM_KEY = "$sms_AssignedFromUplink";
    public static final String CACHE_KEY = "sms_PseudocorePenaltyCache";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var fm = stats.getFleetMember();
        if (fm == null || fm.getFleetData() == null) return;
        if (fm.getFleetCommander() == null || !fm.getFleetCommander().isPlayer()) return;

        var captain = fm.getCaptain();
        if (captain == null || !captain.getMemoryWithoutUpdate().getBoolean(USED_UPLINK_MEM_KEY)) {
            // Removing a hullmod inside applyEffects is undefined behavior as it's concurrent modification
            // Need to do this in another call stack
            DeferredActionPlugin.performLater(() ->
                    fm.getVariant().removePermaMod("sms_pseudocore_uplink_handler")
                    , 0f);
            return;
        }

        PseudocoreUplinkPlugin.PseudocoreUplinkData penaltyData = (PseudocoreUplinkPlugin.PseudocoreUplinkData) fm.getFleetData()
                .getCacheClearedOnSync()
                .computeIfAbsent(CACHE_KEY, k -> PseudocoreUplinkPlugin.getPseudocoreCRPointsAndPenalty());

        float penalty = penaltyData.crPenalty();
        if (penalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Items.uplinkPenaltyDesc);
        }
    }

    @Override
    public void onPlayerFleetSync() {
        // Need to add back because it gets removed when the ship is recovered (and the captain is very temporarily displaced)
        Global.getSector().getPlayerFleet()
                .getFleetData()
                .getMembersListCopy()
                .forEach(fm -> {
                    var captain = fm.getCaptain();
                    if (captain != null && captain.getMemoryWithoutUpdate().getBoolean(USED_UPLINK_MEM_KEY)) {
                        CampaignUtils.addPermaModCloneVariantIfNeeded(fm, "sms_pseudocore_uplink_handler", false);
                    }
                });
    }
}
