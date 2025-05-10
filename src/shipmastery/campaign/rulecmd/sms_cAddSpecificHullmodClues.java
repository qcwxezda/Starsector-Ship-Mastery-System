package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cAddSpecificHullmodClues extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        var memory = getEntityMemory(memoryMap);
        if (memory == null) return false;

        int id = memory.getInt(Strings.Campaign.HULLMOD_NUM_TO_ADD);
        var panel = dialog.getTextPanel();
        var strings = Strings.Campaign.rearrangementHints;
        if (id-1 < 0 || id-1 >= strings.length) return false;
        panel.addPara(strings[id-1]);
        return true;
    }
}
