package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class sms_GenerateStationDefenses extends BaseCommandPlugin  {

    public static final String FLEET_TYPE_MEMORY_KEY = "$sms_FleetType";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        SectorEntityToken entity = dialog.getInteractionTarget();
        String specId = entity.getCustomEntityType();
        MemoryAPI memory = entity.getMemoryWithoutUpdate();
        String fleetType = memory.getString(FLEET_TYPE_MEMORY_KEY);
        if (fleetType == null) return false;

        switch (fleetType) {
            case "XIV": break;
            case "pirates": break;

        }


        return true;
    }
}
