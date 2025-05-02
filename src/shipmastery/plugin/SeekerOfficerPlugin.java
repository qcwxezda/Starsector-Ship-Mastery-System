package shipmastery.plugin;

import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.BaseGenerateFleetOfficersPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.HashMap;
import java.util.Random;

public class SeekerOfficerPlugin extends BaseGenerateFleetOfficersPlugin {
    @Override
    public void addCommanderAndOfficers(CampaignFleetAPI fleet, FleetParamsV3 params, Random random) {
        boolean shouldIntegrate = !params.doNotIntegrateAICores;
        int numCommanderSkills = switch (params.aiCores) {
            case AI_OMEGA -> 7;
            case AI_ALPHA -> 5;
            case AI_BETA -> 3;
            case AI_BETA_OR_GAMMA -> 2;
            case AI_GAMMA -> 1;
            default -> 0;
        };

        WeightedRandomPicker<String> officerPicker = new WeightedRandomPicker<>();
        switch (params.aiCores) {
            case AI_GAMMA: {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add(null, 1f);
                break;
            }
            case AI_BETA_OR_GAMMA: {
                officerPicker.add("sms_fractured_gamma_core", 1f);
                officerPicker.add("sms_subknowledge_core", 1f);
                officerPicker.add(null, 1f);
                break;
            }
            case AI_BETA: {
                officerPicker.add("sms_fractured_gamma_core", 0.25f);
                officerPicker.add("sms_subknowledge_core", 1f);
                officerPicker.add("sms_knowledge_core", 0.5f);
                officerPicker.add(null, 0.5f);
                break;
            }
            case AI_ALPHA: {
                officerPicker.add("sms_fractured_gamma_core", 0.5f);
                officerPicker.add("sms_subknowledge_core", 1f);
                officerPicker.add("sms_knowledge_core", 2f);
                officerPicker.add("sms_amorphous_core", 1f);
                break;
            }
            case AI_OMEGA: {
                officerPicker.add("sms_amorphous_core", 1f);
                break;
            }
        }

        var pluginCache = new HashMap<String, AICoreOfficerPlugin>();
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            String coreId = officerPicker.pick();
            if (coreId == null) continue;
            var plugin = pluginCache.computeIfAbsent(coreId, k -> Misc.getAICoreOfficerPlugin(coreId));
            if (plugin == null) continue;
            fm.setCaptain(plugin.createPerson(coreId, "sms_seeker", random));
            if (shouldIntegrate) {
                RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fm);
            }
        }
    }

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof GenerateFleetOfficersPickData data)) {
            return -1;
        } else if (data.params == null || !data.params.withOfficers) {
            return -1;
        } else {
            return data.fleet != null && "sms_seeker".equals(data.fleet.getFaction().getId()) ? 1000 : -1;
        }
    }
}