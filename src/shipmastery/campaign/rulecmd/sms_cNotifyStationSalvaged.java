package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievementManager;
import shipmastery.achievements.SalvagedAllStations;
import shipmastery.achievements.SalvagedStation;
import shipmastery.procgen.Generator;
import shipmastery.util.Strings;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class sms_cNotifyStationSalvaged extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        int id = getEntityMemory(memoryMap).getInt(Strings.Campaign.BEACON_ID);
        var globalMemory = memoryMap.get(MemKeys.GLOBAL);
        //noinspection unchecked
        Set<Integer> ids = (Set<Integer>) globalMemory.get(Strings.Campaign.SALVAGED_BEACON_IDS);
        if (ids == null) {
            ids = new HashSet<>();
            globalMemory.set(Strings.Campaign.SALVAGED_BEACON_IDS, ids);
        }
        ids.add(id);

        // Check for achievement completion
        MagicAchievementManager.getInstance().completeAchievement(SalvagedStation.class);
        if (ids.size() >= Generator.NUM_STATIONS_HULLMOD + Generator.NUM_STATIONS_ITEM) {
            MagicAchievementManager.getInstance().completeAchievement(SalvagedAllStations.class);
        }

        return true;
    }
}
