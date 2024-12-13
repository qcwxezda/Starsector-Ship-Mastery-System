package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.FleetInflationListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.loading.specs.HullVariantSpec;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.PlayerMPHandler;
import shipmastery.combat.CombatListenerManager;
import shipmastery.config.Settings;
import shipmastery.util.SizeLimitedMap;

import java.util.*;

public class RecentBattlesTracker extends BaseCampaignEventListener implements FleetInflationListener {

    private final Map<FleetMemberAPI, ShipVariantAPI> originalVariantMap = new HashMap<>();
    private final Map<FleetMemberAPI, PersonAPI> captainMap = new HashMap<>();
    private BattleCreationContext firstContextOfBattle;

    /** Necessary to keep track of inflaters because some inflaters aren't persistent are deleted immediately upon inflation. *
     *  Also don't want to blindly set removeAfterInflating to false on every fleet, as that would add lots of unnecessary
     *  inflaters to the save file. */
    private final SizeLimitedMap<CampaignFleetAPI, FleetInflater> inflaterMap =
            new SizeLimitedMap<>(1000);
    private boolean battleIsAutoPursuit = true;

    public RecentBattlesTracker() {
        super(false);
    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {
        BattleAPI battle = result.getBattle();
        if (battle == null) return;
        if (!Settings.ENABLE_RECENT_BATTLES) return;
        if (firstContextOfBattle == null)
            firstContextOfBattle = CombatListenerManager.getLastBattleCreationContext();

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

    private boolean shouldSaveBattle(CampaignFleetAPI primaryWinner) {
        if (!Misc.isPlayerOrCombinedContainingPlayer(primaryWinner)) return false;
        if (battleIsAutoPursuit) return false;
        return Settings.ENABLE_RECENT_BATTLES;
    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {
        // TODO: What if a retreating fleet merges with a larger one, causing two different recent battle intel entries
        // to have the same officer? Is cloning officers necessary due to this?
        if (!battle.isPlayerInvolved()) return;
        if (shouldSaveBattle(primaryWinner)) {
            List<CampaignFleetAPI> snapshot = new ArrayList<>(battle.getNonPlayerSideSnapshot());
            CampaignFleetAPI primary = battle.getPrimary(snapshot);
            if (primary == null) return;
            // Ensure that the primary fleet is first in the snapshot list
            if (snapshot.remove(primary)) {
                snapshot.add(0, primary);
            }

            List<CampaignFleetAPI> fleetsCopy = new ArrayList<>();
            for (CampaignFleetAPI fleetToCopy : snapshot) {
                CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(
                        fleetToCopy.getFaction().getId(),
                        fleetToCopy.getNameWithFactionKeepCase(),
                        true);
                fleet.setName(fleetToCopy.getName());
                for (FleetMemberAPI fm : fleetToCopy.getFleetData().getSnapshot()) {
                    HullVariantSpec variant = (HullVariantSpec) originalVariantMap.get(fm);
                    if (variant == null) variant = (HullVariantSpec) fm.getVariant().clone();

                    if (Settings.RECENT_BATTLES_PRECISE_MODE) {
                        // So that the variant gets saved to file
                        variant.setSource(VariantSource.REFIT);
                        for (String id : variant.getModuleSlots()) {
                            ShipVariantAPI moduleVariant = variant.getModuleVariant(id).clone();
                            moduleVariant.setSource(VariantSource.REFIT);
                            moduleVariant.setOriginalVariant(null);
                            variant.setModuleVariant(id, moduleVariant);
                        }
                        // Prevent deflation on save
                        variant.setOriginalVariant(null);
                    }

                    FleetMemberAPI copy = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
                    PersonAPI captain = captainMap.get(fm);
                    if (captain == null) captain = fm.getCaptain();
                    copy.setCaptain(captain);
                    copy.setShipName(fm.getShipName());

                    fleet.getFleetData().addFleetMember(copy);
                    if (captain != null && !captain.isDefault()) {
                        fleet.getFleetData().addOfficer(captain);
                    }
                    copy.getRepairTracker().setCR(copy.getRepairTracker().getMaxCR());
                }
                PersonAPI commander = fleetToCopy.getCommander();
                // Save progression level at which fleet was fought
                if (commander != null) {
                    commander.getMemoryWithoutUpdate().set(FleetHandler.CUSTOM_PROGRESSION_KEY, PlayerMPHandler.getDifficultyProgression());
                }
                fleet.setCommander(commander);
                fleet.setStationMode(fleetToCopy.isStationMode());
                if (!Settings.RECENT_BATTLES_PRECISE_MODE) {
                    FleetInflater inflater = fleetToCopy.getInflater();
                    if (inflater == null) inflater = inflaterMap.get(fleetToCopy);
                    if (inflater != null) {
                        inflater.setRemoveAfterInflating(false);
                    }
                    fleet.setInflater(inflater);
                }
                fleet.setDoNotAdvanceAI(true);
                fleet.setMemory(fleetToCopy.getMemoryWithoutUpdate());
                fleetsCopy.add(fleet);
            }

            BattleCreationContext bccStub = cloneContextAlwaysAttack(firstContextOfBattle, null, null);
            if (bccStub == null) {
                bccStub = new BattleCreationContext(null, FleetGoal.ATTACK, null, FleetGoal.ATTACK);
            }
            addBattleIntel(bccStub, fleetsCopy);
        }

        originalVariantMap.clear();
        captainMap.clear();
        battleIsAutoPursuit = true;
        firstContextOfBattle = null;
    }

    private void addBattleIntel(BattleCreationContext bcc, List<CampaignFleetAPI> fleets) {
        IntelManagerAPI intelManager = Global.getSector().getIntelManager();

        List<IntelInfoPlugin> intelList = intelManager.getIntel(RecentBattlesIntel.class);
        List<IntelInfoPlugin> nonImportant = new ArrayList<>();
//        Set<String> seenCommanderIds = new HashSet<>();
        for (IntelInfoPlugin intel : intelList) {
            if (!intel.isImportant()) {
                nonImportant.add(intel);
            }
//            if (intel instanceof RecentBattlesIntel) {
//                seenCommanderIds.add(((RecentBattlesIntel) intel).getCombinedCommander().getId());
//            }
        }

//        CampaignFleetAPI primary = fleets.get(0);
//        if (seenCommanderIds.contains(primary.getCommander().getId())) {
//            return;
//        }

        RecentBattlesIntel newIntel = new RecentBattlesIntel(
                Settings.RECENT_BATTLES_PRECISE_MODE,
                fleets,
                bcc,
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

    @Override
    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        inflaterMap.remove(fleet);
    }

    @Override
    public void reportFleetInflated(CampaignFleetAPI fleet, FleetInflater inflater) {
        inflaterMap.put(fleet, inflater);
    }

    public static BattleCreationContext cloneContextAlwaysAttack(BattleCreationContext clone, CampaignFleetAPI newPlayerFleet, CampaignFleetAPI newEnemyFleet) {
        if (clone == null) return null;
        BattleCreationContext newContext = new BattleCreationContext(newPlayerFleet, FleetGoal.ATTACK, newEnemyFleet, FleetGoal.ATTACK);
        newContext.setPlayerCommandPoints(clone.getPlayerCommandPoints());
        newContext.setPursuitRangeModifier(clone.getPursuitRangeModifier());
        newContext.setEscapeDeploymentBurnDuration(clone.getEscapeDeploymentBurnDuration());
        newContext.setFlankDeploymentDistance(clone.getFlankDeploymentDistance());
        newContext.setInitialEscapeRange(clone.getInitialEscapeRange());
        newContext.setInitialNumSteps(clone.getInitialNumSteps());
        newContext.setStandoffRange(clone.getStandoffRange());
        newContext.setNormalDeploymentBurnDuration(clone.getNormalDeploymentBurnDuration());
        newContext.setInitialDeploymentBurnDuration(clone.getInitialDeploymentBurnDuration());
        newContext.setInitialStepSize(clone.getInitialStepSize());
        newContext.objectivesAllowed = clone.objectivesAllowed;
        newContext.aiRetreatAllowed = clone.aiRetreatAllowed;
        newContext.enemyDeployAll = clone.enemyDeployAll;
        newContext.fightToTheLast = clone.fightToTheLast;
        return newContext;
    }
}
