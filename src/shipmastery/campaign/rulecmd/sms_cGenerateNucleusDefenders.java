package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class sms_cGenerateNucleusDefenders extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var entity = dialog.getInteractionTarget();
        if (entity == null) return false;


        var mem = getEntityMemory(memoryMap);
        CampaignFleetAPI defenders = mem.getFleet("$defenderFleet");
        if (defenders != null && !defenders.isEmpty()) {
            return true;
        }

        String commanderId = entity.getMemoryWithoutUpdate().getString(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY);
        if (commanderId == null) {
            commanderId = Misc.genUID();
            entity.getMemoryWithoutUpdate().set(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY, commanderId);
        }

        FleetParamsV3 fParams = new FleetParamsV3(
                null,
                null,
                "sms_curator",
                2f,
                Strings.Campaign.NUCLEUS_DEFENDER_FLEET_TYPE,
                850f, 0f, 0f, 0f, 0f, 0f, 0f);
        fParams.withOfficers = true;
        fParams.aiCores = HubMissionWithTriggers.OfficerQuality.AI_BETA;
        fParams.maxNumShips = 70;
        fParams.maxShipSize = 2;
        fParams.averageSMods = 2;
        fParams.random = new Random(commanderId.hashCode());
        fParams.modeOverride = FactionAPI.ShipPickMode.PRIORITY_ONLY;
        fParams.addShips = new ArrayList<>();

        defenders = FleetFactoryV3.createFleet(fParams);
        defenders.getInflater().setRemoveAfterInflating(false);
        defenders.setName(Strings.Campaign.swarm);
        defenders.clearAbilities();
        defenders.getFleetData().sort();
        if (defenders.getCommander() != null) {
            defenders.getCommander().setId(commanderId);
        }
        for (FleetMemberAPI member : defenders.getFleetData().getMembersListCopy()) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }

        defenders.getLocation().set(entity.getLocation());

        Global.getSector().getPlayerFaction().ensureAtBest("sms_curator", RepLevel.HOSTILE);
        defenders.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        mem.set("$defenderFleet", defenders, 0f);
        return true;
    }
}
