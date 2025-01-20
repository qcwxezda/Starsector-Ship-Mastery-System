package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CoreUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.campaign.fleet.FleetMemberStatus;
import com.fs.starfarer.combat.CombatEngine;
import org.apache.log4j.Logger;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RecentBattlesReplay {
    public static Class<?> encounterDialogClass;
    public static Constructor<?> encounterDialogConstructor;
    public static Field coreUIInEncounterDialogField;
    public static final Logger logger = Logger.getLogger(RecentBattlesReplay.class);
    public static final String isReplayKey = "shipmastery_IsBattleReplay";

    @SuppressWarnings("unused")
    public static void findInteractionDialogClassIfNeeded(IntelUIAPI intelUI) {
        if (encounterDialogClass != null) return;
        intelUI.showDialog(null, "dummy string");
        try {
            Object coreUI = ReflectionUtils.getCoreUI();
            List<?> childrenNonCopy = (List<?>) ReflectionUtils.invokeMethodNoCatch(coreUI, "getChildrenNonCopy");
            if (childrenNonCopy == null) return;
            for (int i = childrenNonCopy.size() - 1; i >= 0; i--) {
                Object child = childrenNonCopy.get(i);
                if (child instanceof InteractionDialogAPI) {
                    for (Constructor<?> cons : child.getClass().getConstructors()) {
                        if (cons.getParameterTypes().length == 4) {
                            encounterDialogClass = child.getClass();
                            for (Field field : encounterDialogClass.getDeclaredFields()) {
                                if (CoreUIAPI.class.isAssignableFrom(field.getType())) {
                                    coreUIInEncounterDialogField = field;
                                    coreUIInEncounterDialogField.setAccessible(true);
                                    break;
                                }
                            }
                            encounterDialogConstructor = cons;
                            ((InteractionDialogAPI) child).dismiss();
                            return;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed to extract interaction dialog", e);
        }
    }

    private static void makeAndSetTempBattleDialog(final CampaignState campaignState, final Action onBackFromEngagement)
            throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        final Map<FleetMember, FleetMemberStatus> savedStatuses = new HashMap<>();
        final Map<FleetMember, Float> savedCR = new HashMap<>();
        final Field fleetMemberStatusField;
        final Field enemyFleetForBattleField;
        final long lastPlayerBattleTimestamp = Global.getSector().getLastPlayerBattleTimestamp();
        final boolean lastPlayerBattleWon = Global.getSector().isLastPlayerBattleWon();
        final Object previousEncounterDialog = campaignState.getCurrentInteractionDialog();

        try {
            fleetMemberStatusField = FleetMember.class.getDeclaredField("status");
            fleetMemberStatusField.setAccessible(true);
            enemyFleetForBattleField = CampaignState.class.getDeclaredField("enemyFleetForBattle");
            enemyFleetForBattleField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        final Map<FleetMemberAPI, PersonAPI> origCaptains = new HashMap<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            origCaptains.put(member, member.getCaptain());
        }

        InteractionDialogPlugin battlePlugin = new InteractionDialogPlugin() {
            private void setFleetMemberStatus(FleetMember fm, FleetMemberStatus status) {
                try {
                    fleetMemberStatusField.set(fm, status);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            private void unsetEnemyFleetForBattle(CampaignState campaignState) {
                try {
                    enemyFleetForBattleField.set(campaignState, null);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void init(InteractionDialogAPI dialog) {
                for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                    FleetMember member = (FleetMember) fm;
                    savedStatuses.put(member, member.getStatus());
                    setFleetMemberStatus(member, new FleetMemberStatus(member));
                    // Phase anchor modifies CR, so track CR as well
                    savedCR.put(member, member.getRepairTracker().getCR());
                }
            }
            @Override
            public void optionSelected(String optionText, Object optionData) {}
            @Override
            public void optionMousedOver(String optionText, Object optionData) {}
            @Override
            public void advance(float amount) {}
            @Override
            public void backFromEngagement(EngagementResultAPI battleResult) {
                // Restore original captains
                for (FleetMemberAPI member : origCaptains.keySet()) {
                    PersonAPI captain = origCaptains.get(member);
                    if (captain != null) {
                        member.setCaptain(captain);
                    }
                }
                // Note: no need to call previous dialog's backFromEngagement, this is a fake engagement
                // and shouldn't count anyway
                // Also, battleResult is malformed; battleResult.getBattle() is null
                // Discard this dialog and plugin, set campaign dialog to previous dialog
                ReflectionUtils.invokeMethodExtWithClasses(
                        campaignState,
                        "setEncounterDialog",
                        false,
                        new Class<?>[] {encounterDialogClass},
                        previousEncounterDialog);
                // Set enemy fleet to null to prevent music change
                unsetEnemyFleetForBattle(campaignState);
                // Set all player ships to their original states
                for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                    FleetMember member = (FleetMember) fm;
                    setFleetMemberStatus(member, savedStatuses.get(member));
                    Float cr = savedCR.get(member);
                    member.getRepairTracker().setCR(cr == null ? member.getRepairTracker().getMaxCR() : cr);
                    member.updateStats();
                }
                // Reset last player battle timestamp and whether last battle was a player win,
                // as this fight shouldn't count.
                // Do it outside the current call stack, as CampaignState.prepare sets the values after calling backFromEngagement
                DeferredActionPlugin.performLater(new Action() {
                    @Override
                    public void perform() {
                        Global.getSector().setLastPlayerBattleTimestamp(lastPlayerBattleTimestamp);
                        Global.getSector().setLastPlayerBattleWon(lastPlayerBattleWon);
                        onBackFromEngagement.perform();
                    }
                }, 0f);
            }
            @Override
            public Object getContext() {return null;}
            @Override
            public Map<String, MemoryAPI> getMemoryMap() {return null;}
        };

        Global.getSector().setPaused(true);
        Object screenPanel = ReflectionUtils.getField(campaignState, "screenPanel");
        Object newDialog = encounterDialogConstructor.newInstance(null, battlePlugin, screenPanel, campaignState);
        if (coreUIInEncounterDialogField != null) {
            coreUIInEncounterDialogField.set(newDialog, ReflectionUtils.getCoreUI());
        }
        ReflectionUtils.invokeMethodNoCatch(campaignState, "setEncounterDialog", newDialog);
    }


    @SuppressWarnings("unused")
    public static void replayBattle(BattleCreationContext bcc, Action onBackFromEngagement) {
        final CampaignFleetAPI fleet = bcc.getOtherFleet();
        try {
            final CampaignState campaignState = (CampaignState) Global.getSector().getCampaignUI();
            makeAndSetTempBattleDialog(campaignState, onBackFromEngagement);
            campaignState.startBattle(bcc);
            final CombatEngine engine = CombatEngine.getInstance();
            engine.getCustomData().put(isReplayKey, true);
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                private boolean removedConfirm = false;
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (engine.isEnemyInFullRetreat() && !removedConfirm) {
                        removedConfirm = true;
                        engine.setCustomExit(Strings.RecentBattles.exitReplay, null);
                    }
                }
                @Override
                public void init(CombatEngineAPI eng) {
                    engine.setCustomExit(Strings.RecentBattles.exitReplay, Strings.RecentBattles.confirmExitReplay);
                }
            });
        }
        catch (Exception e) {
            logger.error("Replay battle failed: ", e);
        }
    }
}
