package shipmastery.procgen;

import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BaseGenericPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageGenFromSeed;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class StationDefenderPlugin extends BaseGenericPlugin implements SalvageGenFromSeed.SalvageDefenderModificationPlugin{
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
        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            fm.setCaptain(plugin.createPerson(Commodities.ALPHA_CORE, fleet.getFaction().getId(), random));
            RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fm);
        }
        PersonAPI commander = fleet.getFlagship().getCaptain();
        commander.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
        commander.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
        commander.getStats().setSkillLevel(Skills.BEST_OF_THE_BEST, 1);
        fleet.setCommander(commander);
        Map<String, Map<Integer, Boolean>> masteries = new HashMap<>();
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            fm.getRepairTracker().setCR(fm.getRepairTracker().getMaxCR());
            ShipHullSpecAPI spec = Utils.getRestoredHullSpec(fm.getHullSpec());
            Map<Integer, Boolean> levels = new HashMap<>();
            for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
                List<MasteryEffect> option2 = ShipMastery.getMasteryEffects(spec, i, true);
                boolean isOption2 = Misc.random.nextBoolean();
                levels.put(i, !option2.isEmpty() && isOption2);
            }
            masteries.put(spec.getHullId(), levels);
        }
        commander.getMemoryWithoutUpdate().set(FleetHandler.CUSTOM_MASTERIES_KEY, masteries, 30f);
    }

    @Override
    public void reportDefeated(SalvageGenFromSeed.SDMParams p, SectorEntityToken entity, CampaignFleetAPI fleet) {}

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
