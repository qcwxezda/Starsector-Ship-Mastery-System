package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class sms_cConcealedProbeAddHullmod extends BaseCommandPlugin {

    public static final String TRIED_TO_ADD_HULLMOD = "$sms_TriedToAddHullmod";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var memory = getEntityMemory(memoryMap);
        if (memory.getBoolean(TRIED_TO_ADD_HULLMOD)) return true;
        if (dialog.getInteractionTarget() != null) {
            memory.set(TRIED_TO_ADD_HULLMOD, true);
            int curNum = memoryMap.get(MemKeys.GLOBAL).getInt(Strings.Campaign.NUM_PROBES_PROCESSED);
            var commanderId = memory.getString(Strings.Campaign.DEFENSES_COMMANDER_ID_KEY);
            if (commanderId == null) commanderId = "";
            Random random = new Random(commanderId.hashCode());
            if ((Misc.random.nextFloat() < 1f/3f || curNum == 1 || curNum == 4) && !Global.getSector().getPlayerFaction().knowsHullMod(Strings.Hullmods.ANALYSIS_PACKAGE)) {
                CargoAPI cargo = Global.getFactory().createCargo(true);
                cargo.addHullmods(Strings.Hullmods.ANALYSIS_PACKAGE, 1);
                BaseSalvageSpecial.addExtraSalvage(dialog.getInteractionTarget(), cargo);
            }
            memoryMap.get(MemKeys.GLOBAL).set(Strings.Campaign.NUM_PROBES_PROCESSED, curNum + 1);
        }

        return true;
    }
}
