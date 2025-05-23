package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.deferred.DeferredActionPlugin;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cConcealedStationIncreaseDP extends BaseCommandPlugin {
    public static final String MODIFY_KEY = "sms_ConcealedStationDPBonus";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        var stats = Global.getSector().getPlayerStats();
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).modifyFlat(MODIFY_KEY, 0.2f);
        DeferredActionPlugin.performOnUnpause(() -> stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodify(MODIFY_KEY));

        return true;
    }
}
