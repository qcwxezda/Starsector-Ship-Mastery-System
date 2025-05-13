package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.FleetAdvanceScript;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cNucleusDefenderInteraction extends BaseCommandPlugin {

    // Adapted from SalvageDefenderInteraction
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        final SectorEntityToken entity = dialog.getInteractionTarget();
        final MemoryAPI memory = getEntityMemory(memoryMap);

        final CampaignFleetAPI defenders = memory.getFleet("$defenderFleet");
        if (defenders == null) return false;

        dialog.setInteractionTarget(defenders);
        Global.getSector().getCampaignUI().restartEncounterMusic(defenders);

        final FleetInteractionDialogPluginImpl.FIDConfig config = new FleetInteractionDialogPluginImpl.FIDConfig();
        config.leaveAlwaysAvailable = true;
        config.showFleetAttitude = false;
        config.showTransponderStatus = false;
        config.showWarningDialogWhenNotHostile = false;
        config.alwaysAttackVsAttack = true;
        config.impactsAllyReputation = true;
        config.impactsEnemyReputation = false;
        config.pullInAllies = false;
        config.pullInEnemies = false;
        config.pullInStations = false;
        config.lootCredits = false;

        config.dismissOnLeave = false;
        config.printXPToDialog = true;

        long seed = memory.getString(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY).hashCode();
        config.salvageRandom = Misc.getRandom(seed, 75);

        final FleetInteractionDialogPluginImpl plugin = new FleetInteractionDialogPluginImpl(config);

        final InteractionDialogPlugin originalPlugin = dialog.getPlugin();
        config.delegate = new FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
            @Override
            public void notifyLeave(InteractionDialogAPI dialog) {
                // nothing in there we care about keeping; clearing to reduce savefile size
                defenders.getMemoryWithoutUpdate().clear();
                // there's a "standing down" assignment given after a battle is finished that we don't care about
                defenders.clearAssignments();
                defenders.deflate();

                dialog.setPlugin(originalPlugin);
                dialog.setInteractionTarget(entity);

                //Global.getSector().getCampaignUI().clearMessages();

                if (plugin.getContext() instanceof FleetEncounterContext context) {
                    if (context.didPlayerWinEncounterOutright()) {
                        memory.unset("$defenderFleet");
                        memory.set("$defenderFleetDefeated", true);
                        entity.removeScriptsOfClass(FleetAdvanceScript.class);
                        FireBest.fire(null, dialog, memoryMap, "BeatDefendersContinue");
                    } else {
                        boolean persistDefenders = false;
                        if (context.isEngagedInHostilities()) {
                            persistDefenders |= !Misc.getSnapshotMembersLost(defenders).isEmpty();
                            for (FleetMemberAPI member : defenders.getFleetData().getMembersListCopy()) {
                                if (member.getStatus().needsRepairs()) {
                                    persistDefenders = true;
                                    break;
                                }
                            }
                        }

                        if (persistDefenders) {
                            if (!entity.hasScriptOfClass(FleetAdvanceScript.class)) {
                                defenders.setDoNotAdvanceAI(true);
                                defenders.setContainingLocation(entity.getContainingLocation());
                                // somewhere far off where it's not going to be in terrain or whatever
                                defenders.setLocation(1000000, 1000000);
                                entity.addScript(new FleetAdvanceScript(defenders));
                            }
                            memory.expire("$defenderFleet", 10); // defenders may have gotten damaged; persist them for a bit
                        }
                        dialog.dismiss();
                    }
                } else {
                    dialog.dismiss();
                }
            }
            @Override
            public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                bcc.aiRetreatAllowed = false;
                bcc.objectivesAllowed = true;
                bcc.forceObjectivesOnMap = true;
            }

            @Override
            public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                FleetEncounterContextPlugin.DataForEncounterSide winner = context.getWinnerData();
                FleetEncounterContextPlugin.DataForEncounterSide loser = context.getLoserData();

                if (winner == null || loser == null) return;
                float playerContribMult = context.computePlayerContribFraction();
                List<SalvageEntityGenDataSpec.DropData> dropRandom = new ArrayList<>();
                List<SalvageEntityGenDataSpec.DropData> dropValue = new ArrayList<>();
                float valueMultFleet = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.BATTLE_SALVAGE_MULT_FLEET);
                float valueModShips = context.getSalvageValueModPlayerShips();
                float fuelMult = Global.getSector().getPlayerFleet().getStats().getDynamic().getValue(Stats.FUEL_SALVAGE_VALUE_MULT_FLEET);
                //float fuel = salvage.getFuel();
                //salvage.addFuel((int) Math.round(fuel * fuelMult));
                CargoAPI extra = SalvageEntity.generateSalvage(config.salvageRandom, valueMultFleet + valueModShips, 1f, 1f, fuelMult, dropValue, dropRandom);
                for (CargoStackAPI stack : extra.getStacksCopy()) {
                    if (stack.isFuelStack()) {
                        stack.setSize((int)(stack.getSize() * fuelMult));
                    }
                    salvage.addFromStack(stack);
                }
            }
        };

        dialog.setPlugin(plugin);
        plugin.init(dialog);
        return true;
    }
}
