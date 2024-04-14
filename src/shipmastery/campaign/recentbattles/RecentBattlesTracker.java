package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import shipmastery.config.Settings;

import java.util.*;

public class RecentBattlesTracker extends BaseCampaignEventListener {

    private final Map<FleetMemberAPI, ShipVariantAPI> originalVariantMap = new HashMap<>();
    private final Map<FleetMemberAPI, PersonAPI> captainMap = new HashMap<>();
    private boolean battleIsAutoPursuit = true;

    public RecentBattlesTracker() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        BattleAPI battle = result.getBattle();
        if (battle == null) return;
        if (!Settings.ENABLE_RECENT_BATTLES) return;

        for (CampaignFleetAPI fleet : battle.getNonPlayerSideSnapshot()) {
            for (FleetMemberAPI fm : fleet.getFleetData().getSnapshot()) {
                if (!originalVariantMap.containsKey(fm)) {
                    originalVariantMap.put(fm, fm.getVariant().clone());
                }
                // Necessary in case some ships are recovered and captains unset
                if (!captainMap.containsKey(fm)) {
                    captainMap.put(fm, fm.getCaptain());
                }
            }
        }

        EngagementResultForFleetAPI playerResult = result.getLoserResult().isPlayer() ? result.getLoserResult() : result.getWinnerResult();
        battleIsAutoPursuit &= playerResult.getAllEverDeployedCopy() == null;
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        if (!Misc.isPlayerOrCombinedContainingPlayer(primaryWinner)) return;
        if (battleIsAutoPursuit) return;
        if (!Settings.ENABLE_RECENT_BATTLES) return;

        List<CampaignFleetAPI> snapshot = battle.getNonPlayerSideSnapshot();
        CampaignFleetAPI primary = battle.getPrimary(snapshot);
        if (primary == null || battle.getNonPlayerCombined() == null) return;

        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(primary.getFaction().getId(), battle.getNonPlayerCombined().getNameWithFactionKeepCase(), true);
        fleet.setName(primary.getName());
        for (CampaignFleetAPI origFleet : snapshot) {
            for (FleetMemberAPI fm : origFleet.getFleetData().getSnapshot()) {
                HullVariantSpec variant = (HullVariantSpec) originalVariantMap.get(fm);
                if (variant == null) variant = (HullVariantSpec) fm.getVariant().clone();

                if (Settings.RECENT_BATTLES_PRECISE_MODE) {
                    // So that the variant gets saved to file
                    variant.setSource(VariantSource.REFIT);
                    for (String id : variant.getModuleSlots()) {
                        ShipVariantAPI moduleVariant = variant.getModuleVariant(id).clone();
                        moduleVariant.setSource(VariantSource.REFIT);
                        variant.setModuleVariant(id, moduleVariant);
                    }
                    // Prevent deflation on save
                    variant.setOriginalVariant(null);
                }

                FleetMemberAPI copy = new FleetMember(1, variant, FleetMemberType.SHIP);
                PersonAPI captain = captainMap.get(fm);
                if (captain == null) captain = fm.getCaptain();
                copy.setCaptain(captain);
                copy.setShipName(fm.getShipName());

                fleet.getFleetData().addFleetMember(copy);
                copy.getRepairTracker().setCR(copy.getRepairTracker().getMaxCR());
            }
        }
        fleet.setCommander(primary.getCommander());
        fleet.setStationMode(primary.isStationMode());

        originalVariantMap.clear();
        captainMap.clear();
        battleIsAutoPursuit = true;

        addBattleIntel(fleet, Settings.RECENT_BATTLES_PRECISE_MODE || primary.getInflater() == null ? null : primary.getInflater().getParams());
    }

    private void addBattleIntel(CampaignFleetAPI fleet, Object fleetInflaterParams) {
        IntelManagerAPI intelManager = Global.getSector().getIntelManager();

        List<IntelInfoPlugin> intelList = intelManager.getIntel(RecentBattlesIntel.class);
        List<IntelInfoPlugin> nonImportant = new ArrayList<>();
        Set<String> seenCommanderIds = new HashSet<>();
        for (IntelInfoPlugin intel : intelList) {
            if (!intel.isImportant()) {
                nonImportant.add(intel);
            }
            if (intel instanceof RecentBattlesIntel) {
                seenCommanderIds.add(((RecentBattlesIntel) intel).getFleetData().getCommander().getId());
            }
        }

        if (seenCommanderIds.contains(fleet.getCommander().getId())) {
            return;
        }

        RecentBattlesIntel newIntel = new RecentBattlesIntel(
                Settings.RECENT_BATTLES_PRECISE_MODE,
                fleet,
                fleetInflaterParams,
                Global.getSector().getPlayerFleet().getContainingLocation());
        intelManager.addIntel(newIntel);
        nonImportant.add(newIntel);

        if (nonImportant.size() > RecentBattlesIntel.MAX_SIZE) {
            Collections.sort(nonImportant, new Comparator<IntelInfoPlugin>() {
                @Override
                public int compare(IntelInfoPlugin info1, IntelInfoPlugin info2) {
                    return Long.compare(info1.getPlayerVisibleTimestamp(), info2.getPlayerVisibleTimestamp());
                }
            });

            for (int i = 0; i < nonImportant.size() - RecentBattlesIntel.MAX_SIZE; i++) {
                IntelInfoPlugin intel = nonImportant.get(i);
                if (intel instanceof RecentBattlesIntel) {
                    RecentBattlesIntel rIntel = (RecentBattlesIntel) intel;
                    intelManager.removeIntel(rIntel);
                }
            }
        }
    }
}
