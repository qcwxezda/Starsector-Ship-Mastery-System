package shipmastery.plugin;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.GenerateFleetOfficersPlugin;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ConcealedEntityDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {
    @Override
    public float getStrength(SalvageGenFromSeed.SDMParams p, float strength, Random random, boolean withOverride) {
        if (entityIsProbe(p)) return MathUtils.randBetween(20f, 60f, random);
        int defeatedNum = Global.getSector().getMemoryWithoutUpdate().getInt(Strings.Campaign.NUM_STATIONS_DEFEATED);
        return switch (defeatedNum) {
            case 0 -> 150f;
            case 1 -> 180f;
            case 2 -> 240f;
            case 3 -> 300f;
            default -> 360f;
        };
    }

    @Override
    public float getProbability(SalvageGenFromSeed.SDMParams p, float probability, Random random, boolean withOverride) {
        return entityIsProbe(p) ? 0.5f : 1f;
    }

    @Override
    public float getQuality(SalvageGenFromSeed.SDMParams p, float quality, Random random, boolean withOverride) {
        return entityIsProbe(p) ? 0.4f : 2f;
    }

    @Override
    public float getMaxSize(SalvageGenFromSeed.SDMParams p, float maxSize, Random random, boolean withOverride) {
        return entityIsProbe(p) ? 2f : 1000f;
    }

    @Override
    public float getMinSize(SalvageGenFromSeed.SDMParams p, float minSize, Random random, boolean withOverride) {
        return 0f;
    }

    private boolean entityIsProbe(SalvageGenFromSeed.SDMParams p) {
        return "sms_concealed_probe".equals(p.entity.getCustomEntitySpec().getId());
    }

    @Override
    public void modifyFleet(SalvageGenFromSeed.SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
        FactionAPI temp = Global.getSettings().createBaseFaction("sms_curator");
        fleet.setFaction(temp.getId());
        FleetParamsV3 params = new FleetParamsV3();


        if (entityIsProbe(p)) {
            params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_GAMMA;
        } else {
            int numDefeated = Global.getSector().getMemoryWithoutUpdate().getInt(Strings.Campaign.NUM_STATIONS_DEFEATED);
            switch (numDefeated) {
                case 0 -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_BETA_OR_GAMMA;
                case 1 -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_BETA;
                case 2 -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_MIXED;
                case 3 -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_ALPHA;
                default -> params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_OMEGA;
            }
        }

        // Adding officers may change maximum CR, but we don't want to fully repair in case the fleet
        // started with non-maximum CR. So only add the difference.
        Map<FleetMemberAPI, Float> initialMaxCRMap = new HashMap<>();
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            initialMaxCRMap.put(fm, fm.getRepairTracker().getMaxCR());
        }

        var pickData = new GenerateFleetOfficersPlugin.GenerateFleetOfficersPickData(fleet, params);
        var plugin = Global.getSector().getGenericPlugins().pickPlugin(GenerateFleetOfficersPlugin.class, pickData);
        plugin.addCommanderAndOfficers(fleet, params, random);

        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            Float initial = initialMaxCRMap.get(fm);
            if (initial != null) {
                float diff = fm.getRepairTracker().getMaxCR() - initial;
                fm.getRepairTracker().setCR(fm.getRepairTracker().getCR() + diff);
            }
        }

        // Set the commander's id to seed the masteries properly
        if (fleet.getCommander() != null) {
            String commanderId = p.entity.getMemoryWithoutUpdate().getString(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY);
            if (commanderId != null) {
                fleet.getCommander().setId(commanderId);
            }
        }
    }

    @Override
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
        if (entityIsProbe(p)) return;
        var memory = Global.getSector().getMemoryWithoutUpdate();
        int defeatedNum = memory.getInt(Strings.Campaign.NUM_STATIONS_DEFEATED);
        memory.set(Strings.Campaign.NUM_STATIONS_DEFEATED, defeatedNum + 1);
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SalvageGenFromSeed.SDMParams p)) return -1;

        if (p.entity == null || p.entity.getCustomEntitySpec() == null) {
            return -1;
        }

        String id = p.entity.getCustomEntitySpec().getId();
        return "sms_concealed_station".equals(id) || "sms_concealed_probe".equals(id)
                ? GenericPluginManagerAPI.MOD_SPECIFIC
                : -1;
    }
}
