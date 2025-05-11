package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.items.SuperconstructPlugin;
import shipmastery.procgen.TestGenerator;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cConcealedStationAddSuperconstruct extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var memory = getEntityMemory(memoryMap);
        if (memory.getBoolean(Strings.Campaign.STATION_USED_KEY)) return true;

        TestGenerator.StationType type = (TestGenerator.StationType) memory.get(Strings.Campaign.STATION_TYPE_KEY);
        String itemId;
        if (type == TestGenerator.StationType.SUPERCONSTRUCT_1) {
            itemId = Strings.Items.SUPERCONSTRUCT_1;
        } else if (type == TestGenerator.StationType.SUPERCONSTRUCT_2) {
            itemId = Strings.Items.SUPERCONSTRUCT_2;
        } else {
            return false;
        }

        if (dialog.getInteractionTarget() != null) {
            CargoAPI cargo = Global.getFactory().createCargo(true);
            cargo.addSpecial(new SpecialItemData(itemId, SuperconstructPlugin.ACTIVE_STRING), 1f);
            BaseSalvageSpecial.addExtraSalvage(dialog.getInteractionTarget(), cargo);
            memory.set(Strings.Campaign.STATION_USED_KEY, true);
            return true;
        }
        return false;
    }
}
