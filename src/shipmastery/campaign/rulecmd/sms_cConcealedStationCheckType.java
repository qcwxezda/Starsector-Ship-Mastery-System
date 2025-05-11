package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;
import shipmastery.procgen.TestGenerator;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cConcealedStationCheckType extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getInteractionTarget() == null) return false;

        var memory = getEntityMemory(memoryMap);
        if (memory == null) return false;

        TestGenerator.StationType type = (TestGenerator.StationType) memory.get(Strings.Campaign.STATION_TYPE_KEY);
        if (type == null) return false;

        switch (type) {
            case HULLMOD_1 -> {
                memory.set(Strings.Campaign.HULLMOD_NUM_TO_ADD, 1);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationHullmod");
            }
            case HULLMOD_2 -> {
                memory.set(Strings.Campaign.HULLMOD_NUM_TO_ADD, 2);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationHullmod");
            }
            case HULLMOD_3 -> {
                memory.set(Strings.Campaign.HULLMOD_NUM_TO_ADD, 3);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationHullmod");
            }
            case HULLMOD_4 -> {
                memory.set(Strings.Campaign.HULLMOD_NUM_TO_ADD, 4);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationHullmod");
            }
            case HULLMOD_5 -> {
                memory.set(Strings.Campaign.HULLMOD_NUM_TO_ADD, 5);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationHullmod");
            }
            case SUPERCONSTRUCT_1 -> {
                memory.set(Strings.Campaign.SUPERCONSTRUCT_TO_ADD, Strings.Items.SUPERCONSTRUCT_1);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationSuperconstruct");
            }
            case SUPERCONSTRUCT_2 -> {
                memory.set(Strings.Campaign.SUPERCONSTRUCT_TO_ADD, Strings.Items.SUPERCONSTRUCT_2);
                FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationSuperconstruct");
            }
            case CRYO_OFFICER -> FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationOfficer");
        }

        return true;
    }
}
