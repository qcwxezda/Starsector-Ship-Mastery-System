package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cNucleusAddOptions extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var mem = memoryMap.get(MemKeys.GLOBAL);
        boolean isAdmin = mem.getBoolean(Strings.Campaign.NUCLEUS_ADMIN_ACCESS);
        boolean isShutdown = mem.getBoolean(Strings.Campaign.NUCLEUS_SHUT_DOWN);
        boolean isUnsealed = mem.getBoolean(Strings.Campaign.NUCLEUS_UNSEALED);

        dialog.getOptionPanel().clearOptions();
        if (isUnsealed && !getEntityMemory(memoryMap).getBoolean(Strings.Campaign.NUCLEUS_SALVAGED)) {
            dialog.getOptionPanel().addOption(Strings.Campaign.assessForSalvage, "sms_oNucleusAssessSalvage");
        }

        dialog.getOptionPanel().addOption(Strings.Campaign.checkStatus, "sms_oNucleusCheckBeacons");
        if (!isShutdown) {
            dialog.getOptionPanel().addOption(Strings.Campaign.shutDown, "sms_oNucleusShutDown");
        }
        else {
            dialog.getOptionPanel().addOption(Strings.Campaign.powerOn, "sms_oNucleusPowerOn");
        }

        if (!isUnsealed) {
            dialog.getOptionPanel().addOption(Strings.Campaign.unsealStructure, "sms_oNucleusOpenStructure");
        } else {
            dialog.getOptionPanel().addOption(Strings.Campaign.sealStructure, "sms_oNucleusCloseStructure");
        }

        if (!isAdmin) {
            dialog.getOptionPanel().addOption(Strings.Campaign.elevate, "sms_oNucleusAdmin");
        }

        dialog.getOptionPanel().addOption(Strings.Campaign.leave, "defaultLeave");
        return true;
    }
}
