package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.graveyard.ShipGraveyardSpawner;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cAddAICoreFromWreckToLoot extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var mem = getEntityMemory(memoryMap);
        var id = mem.getString(ShipGraveyardSpawner.AI_CORE_MEM_KEY);
        if (id == null) return false;
        var entity = dialog.getInteractionTarget();
        if (entity == null) return false;
        var cargo = Global.getFactory().createCargo(true);
        cargo.addCommodity(id, 1);
        BaseSalvageSpecial.addExtraSalvage(entity, cargo);
        return true;
    }
}
