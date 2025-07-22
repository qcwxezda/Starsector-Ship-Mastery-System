package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.PseudocoreUplinkPlugin;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.CampaignUtils;
import shipmastery.util.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PseudocoreUplinkHullmod extends BaseHullMod implements PlayerFleetSyncListener {

    public static final String USED_UPLINK_MEM_KEY = "$sms_AssignedFromUplink";
    public static PseudocoreUplinkPlugin.PseudocoreUplinkData penaltyData =
            new PseudocoreUplinkPlugin.PseudocoreUplinkData(0f, 0f, 0f);
    // Fleet member id to AI core id
    private static final Map<String, String> prevOfficerData = new HashMap<>();
    private static Map<String, Integer> prevCargoCounts = new HashMap<>();
    private static Map<String, Integer> cargoDiffs = new HashMap<>();

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var fm = stats.getFleetMember();
        if (fm == null || fm.getFleetData() == null) {
            return;
        }
        if (fm.getFleetCommander() == null || !fm.getFleetCommander().isPlayer()) {
            return;
        }
        // Should only happen if some mod has a feature to automate a ship
        if (Misc.isAutomated(stats)) {
            return;
        }

        var captain = fm.getCaptain();
        if (captain == null || !captain.getMemoryWithoutUpdate().contains(USED_UPLINK_MEM_KEY)) {
            String origId = prevOfficerData.get(fm.getId());
            // Removing a hullmod inside applyEffects is undefined behavior as it's concurrent modification
            // Need to do this in another call stack
            DeferredActionPlugin.performLater(() -> {
                if (fm.getVariant().hasHullMod("sms_pseudocore_uplink_handler")) {
                    if (origId != null) {
                        // Make sure that the AI core wasn't added already, such as by
                        // right-click remove or by another mod
                        int diff = cargoDiffs.getOrDefault(origId, 0);
                        if (diff <= 0) {
                            Global.getSector().getPlayerFleet().getCargo().addCommodity(origId, 1f);
                            cargoDiffs.put(origId, diff + 1);
                        }
                    }
                    fm.getVariant().removePermaMod("sms_pseudocore_uplink_handler");
                }
            }, 0f);
            return;
        }

        //penaltyData = PseudocoreUplinkPlugin.getPseudocoreCRPointsAndPenalty();
        float penalty = penaltyData.crPenalty();
        if (penalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Items.uplinkPenaltyDesc);
        }
    }

    @Override
    public void onPlayerFleetSync() {
        var membersList = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        var cargoCounts = CampaignUtils.getPlayerCommodityCounts(x -> Misc.getAICoreOfficerPlugin(x.getId()) != null)
                .entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getId(), Map.Entry::getValue));
        cargoDiffs = Stream.concat(prevCargoCounts.entrySet().stream()
                        .map(x -> Map.entry(x.getKey(), -x.getValue())), cargoCounts.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
        prevCargoCounts = cargoCounts;

        prevOfficerData.clear();
        membersList.forEach(fm -> {
            var captain = fm.getCaptain();
            if (captain != null && captain.getMemoryWithoutUpdate().contains(USED_UPLINK_MEM_KEY)) {
                // Need to add back because it gets removed when the ship is recovered (and the captain is very
                // temporarily displaced)
                CampaignUtils.addPermaModCloneVariantIfNeeded(fm, "sms_pseudocore_uplink_handler", false);
                if (!Misc.isUnremovable(captain) && captain.getAICoreId() != null) {
                    prevOfficerData.put(fm.getId(), captain.getAICoreId());
                }
            }
        });

        // Compute the penalty here instead of applyEffectsBeforeShipCreation using cacheClearedOnSync, as otherwise
        // mastery effects modifying DP,
        // which occur later, won't be seen
        penaltyData = PseudocoreUplinkPlugin.getPseudocoreCRPointsAndPenalty();
        // Need to reapply the penalty -- otherwise the updates won't be in time,
        // such as when clicking to pick up the uplink, which reduces the available points to 0
        for (var fm : membersList) {
            var variant = fm.getVariant();
            if (variant.hasHullMod("sms_pseudocore_uplink_handler")) {
                new PseudocoreUplinkHullmod().applyEffectsBeforeShipCreation(variant.getHullSize(), fm.getStats(),
                        "sms_pseudocore_uplink_handler");
            }
        }
    }
}
