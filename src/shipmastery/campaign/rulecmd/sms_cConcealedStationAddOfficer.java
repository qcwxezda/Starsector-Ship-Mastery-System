package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cConcealedStationAddOfficer extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var memory = getEntityMemory(memoryMap);
        memory.set(Strings.Campaign.STATION_USED_KEY, true);

        PersonAPI officer = Global.getSector().getFaction(Factions.PLAYER).createRandomPerson();
        officer.getStats().setLevel(1);
        officer.getStats().setSkillLevel("sms_shared_knowledge", 1);
        officer.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL, 7);
        officer.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_ELITE_SKILLS, 5);
        Global.getSector().getPlayerFleet().getFleetData().addOfficer(officer);

        dialog.getTextPanel().setFontSmallInsignia();
        dialog.getTextPanel().addPara(
                String.format(Strings.Campaign.officerAddedToFleet, officer.getNameString(), officer.getStats().getLevel()),
                Misc.getPositiveHighlightColor());
        dialog.getTextPanel().setFontInsignia();

        return true;
    }
}
