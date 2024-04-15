package shipmastery.campaign.recentbattles;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.campaign.CampaignState;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.campaign.fleet.FleetMemberStatus;
import com.fs.starfarer.combat.CombatEngine;
import org.apache.log4j.Logger;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.Strings;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class RecentBattlesReplay {
    public static final Logger logger = Logger.getLogger(RecentBattlesReplay.class);
    public static final String isReplayKey = "shipmastery_IsBattleReplay";

    @SuppressWarnings("unused")
    public static void replayBattle(BattleCreationContext bcc) {
        final CampaignFleetAPI fleet = bcc.getOtherFleet();
        try {
            // avoid NPE
            final Field dialogTypeField = CampaignState.class.getDeclaredField("dialogType");
            dialogTypeField.setAccessible(true);
            dialogTypeField.set(Global.getSector().getCampaignUI(), null);
            Global.getSector().getCampaignUI().startBattle(bcc);
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

            final Map<FleetMember, FleetMemberStatus> savedStatuses = new HashMap<>();
            final Map<FleetMember, Float> savedCR = new HashMap<>();
            final Field fleetMemberStatusField = FleetMember.class.getDeclaredField("status");
            fleetMemberStatusField.setAccessible(true);
            for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                FleetMember member = (FleetMember) fm;
                savedStatuses.put(member, member.getStatus());
                fleetMemberStatusField.set(member, new FleetMemberStatus(member));
                // Phase anchor modifies CR, so track CR as well
                savedCR.put(member, member.getRepairTracker().getCR());
            }
            // now manually do what CampaignState.prepare() would have done after combat is over
            // and campaign scripts start running again
            DeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    CampaignState ui = (CampaignState) Global.getSector().getCampaignUI();
                    if (ui.isTransitioningToNextState()) {
                        DeferredActionPlugin.performLater(this, 0.5f);
                    }
                    else {
                        CombatEngine.destroyInstance();
                        CombatEngine.getInstance();
                        // Necessary, otherwise game might think it's a campaign battle result after the next simulation,
                        // look for the encounter dialog plugin, and throw an NPE
                        ui.getSession().remove("campaign battle result");
                        try {
                            // set all player ships to their original states
                            for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
                                FleetMember member = (FleetMember) fm;
                                fleetMemberStatusField.set(member, savedStatuses.get(member));
                                Float cr = savedCR.get(member);
                                member.getRepairTracker().setCR(cr == null ? member.getRepairTracker().getMaxCR() : cr);
                            }
                            Field playerFleetForBattleField =
                                    CampaignState.class.getDeclaredField("playerFleetForBattle");
                            Field enemyFleetForBattleField =
                                    CampaignState.class.getDeclaredField("enemyFleetForBattle");
                            playerFleetForBattleField.setAccessible(true);
                            enemyFleetForBattleField.setAccessible(true);
                            playerFleetForBattleField.set(ui, null);
                            enemyFleetForBattleField.set(ui, null);
                        } catch (Exception e) {
                            logger.error("Replay battle cleanup failed: ", e);
                        }
                    }
                }
            }, 0f);
        }
        catch (Exception e) {
            logger.error("Replay battle failed: ", e);
        }
    }
}
