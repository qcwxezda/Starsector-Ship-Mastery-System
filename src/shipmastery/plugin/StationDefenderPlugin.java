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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StationDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin {
    @Override
    public float getStrength(SalvageGenFromSeed.SDMParams p, float strength, Random random, boolean withOverride) {
        return strength;
    }

    @Override
    public float getProbability(SalvageGenFromSeed.SDMParams p, float probability, Random random, boolean withOverride) {
        return probability;
    }

    @Override
    public float getQuality(SalvageGenFromSeed.SDMParams p, float quality, Random random, boolean withOverride) {
        return 2f;
    }

    @Override
    public float getMaxSize(SalvageGenFromSeed.SDMParams p, float maxSize, Random random, boolean withOverride) {
        return maxSize;
    }

    @Override
    public float getMinSize(SalvageGenFromSeed.SDMParams p, float minSize, Random random, boolean withOverride) {
        return minSize;
    }

    @Override
    public void modifyFleet(SalvageGenFromSeed.SDMParams p, CampaignFleetAPI fleet, Random random, boolean withOverride) {
        FactionAPI temp = Global.getSettings().createBaseFaction("sms_seeker");
        fleet.setFaction(temp.getId());
        FleetParamsV3 params = new FleetParamsV3();
        params.aiCores = HubMissionWithTriggers.OfficerQuality.AI_ALPHA;

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

//        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
//        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
//            fm.setCaptain(plugin.createPerson(Commodities.ALPHA_CORE, fleet.getFaction().getId(), random));
//            RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fm);
//        }
//        PersonAPI commander = fleet.getFlagship().getCaptain();
//        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
//        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
//        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
//        fleet.setCommander(commander);
//        Map<String, Map<Integer, String>> masteries = new HashMap<>();
//        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
//            fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR());
//            ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
//            Map<Integer, String> levels = new HashMap<>();
//            for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
//                String selectedOption = FleetHandler.pickOptionForMasteryLevel(spec, i, random);
//                if (selectedOption != null) {
//                    levels.put(i, selectedOption);
//                }
//            }
//            masteries.put(spec.getHullId(), levels);
//        }
//        commander.getMemoryWithoutUpdate().set(FleetHandler.CUSTOM_MASTERIES_KEY, masteries, 30f);
    }

    @Override
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof SalvageGenFromSeed.SDMParams p)) return -1;

        if (p.entity == null || p.entity.getCustomEntitySpec() == null) {
            return -1;
        }

        return "sms_concealed_station".equals(p.entity.getCustomEntitySpec().getId())
                ? GenericPluginManagerAPI.MOD_SPECIFIC
                : -1;
    }
}
