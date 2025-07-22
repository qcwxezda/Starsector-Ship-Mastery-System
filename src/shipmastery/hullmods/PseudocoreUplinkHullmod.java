package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.PseudocoreUplinkPlugin;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;
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
            return;
        }

        float penalty = penaltyData.crPenalty();
        if (penalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -penalty, Strings.Items.uplinkPenaltyDesc);
        } else {
            stats.getMaxCombatReadiness().unmodify(id);
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

        membersList.forEach(fm -> {
            var captain = fm.getCaptain();
            if (captain == null || !captain.getMemoryWithoutUpdate().contains(USED_UPLINK_MEM_KEY)) {
                String origId = prevOfficerData.get(fm.getId());
                if (origId != null && fm.getVariant().hasHullMod(Strings.Hullmods.UPLINK_HANDLER)) {
                    // Make sure that the AI core wasn't added already, such as by
                    // right-click remove or by another mod
                    int diff = cargoDiffs.getOrDefault(origId, 0);
                    if (diff <= 0) {
                        Global.getSector().getPlayerFleet().getCargo().addCommodity(origId, 1f);
                        cargoDiffs.put(origId, diff + 1);
                        prevCargoCounts.compute(origId, (k, v) -> v == null ? 1 : v + 1);
                    }
                    fm.getVariant().removePermaMod(Strings.Hullmods.UPLINK_HANDLER);
                }
            }
        });
        prevOfficerData.clear();
        membersList.forEach(fm -> {
            var captain = fm.getCaptain();
            if (captain != null && captain.getMemoryWithoutUpdate().contains(USED_UPLINK_MEM_KEY)) {
                String coreId = captain.getAICoreId();
                if (!Misc.isUnremovable(captain) && coreId != null) {
                    prevOfficerData.put(fm.getId(), coreId);
                }
                if (!fm.getVariant().hasHullMod(Strings.Hullmods.UPLINK_HANDLER)) {
                    CampaignUtils.addPermaModCloneVariantIfNeeded(fm, Strings.Hullmods.UPLINK_HANDLER, false);
                    // Consume the AI core if it's used as an officer
                    // But first check that it wasn't already consumed by the function that assigned it
                    int diff = cargoDiffs.getOrDefault(coreId, 0);
                    if (diff >= 0) {
                        Global.getSector().getPlayerFleet().getCargo().removeCommodity(coreId, 1f);
                        cargoDiffs.put(coreId, diff - 1);
                        prevCargoCounts.compute(coreId, (k, v) -> v == null ? -1 : v - 1);
                    }
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
            if (variant.hasHullMod(Strings.Hullmods.UPLINK_HANDLER)) {
                new PseudocoreUplinkHullmod().applyEffectsBeforeShipCreation(variant.getHullSize(), fm.getStats(),
                        Strings.Hullmods.UPLINK_HANDLER);
            }
        }
    }
}
