package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cPlaySoundExt extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        String soundId = params.get(0).getString(memoryMap);
        float pitch = params.get(1).getFloat(memoryMap);
        float volume = params.get(2).getFloat(memoryMap);

        Global.getSoundPlayer().playUISound(soundId, pitch, volume);
        return true;
    }
}
