package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.FleetHandler;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_CheckForFleetScript extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        memoryMap.get(MemKeys.GLOBAL).set("$sms_checkedForFleet", true);
        FireBest.fire(ruleId, dialog, memoryMap, "DialogOptionSelected");
        memoryMap.get(MemKeys.GLOBAL).set("$sms_checkedForFleet", false);
        if (dialog.getInteractionTarget() instanceof CampaignFleetAPI fleet) {
            FleetHandler.addMasteriesToFleet(fleet);
        }
        return true;
    }
}
