package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.graveyard.InsuranceFraudDetector;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class sms_cInsuranceFraudForfeit extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId,  InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        Collection<FleetMemberAPI> members = sms_cInsuranceFraudListShips.getShips();
        Set<String> ids = new HashSet<>();
        for (FleetMemberAPI member : members) {
            ids.add(member.getId());
        }

        dialog.getTextPanel().setFontSmallInsignia();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (ids.contains(member.getId())) {
                Global.getSector().getPlayerFleet().getFleetData().removeFleetMember(member);
                dialog.getTextPanel().addPara(
                        Strings.Graveyard.insuranceLostShip, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), member.getShipName());
            }
        }

        Float additionalCost = (Float) memoryMap.get(MemKeys.LOCAL).get(sms_cInsuranceFraudListShips.ADDITIONAL_COST_KEY);
        if (additionalCost != null) {
            Utils.getPlayerCredits().subtract(additionalCost);
            dialog.getTextPanel().addPara(Strings.Graveyard.insuranceLostCredits, Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getDGSCredits(additionalCost));
        }

        dialog.getTextPanel().setFontInsignia();

        SectorEntityToken target = dialog.getInteractionTarget();
        if (target instanceof CampaignFleetAPI fleet) {
            MemoryAPI memory = fleet.getMemory();
            memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, false);
            memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, false);
            memory.set(InsuranceFraudDetector.FLEET_PAID_KEY, true);

            for (FleetMemberAPI member : members) {
                member.getRepairTracker().setMothballed(true);
                member.setCaptain(null);
                fleet.getFleetData().addFleetMember(member);
            }

            // From RuleBasedInteractionDialogPluginImpl, updatePersonMemory()
            // Refreshes the visual panel to show ships added to the collector's fleet
            PersonAPI person = dialog.getInteractionTarget().getActivePerson();
            if (person != null) {
                memory = person.getMemory();
                memoryMap.put(MemKeys.LOCAL, memory);
                memoryMap.put(MemKeys.PERSON_FACTION, person.getFaction().getMemory());
                memoryMap.put(MemKeys.ENTITY, dialog.getInteractionTarget().getMemory());
            } else {
                memory = dialog.getInteractionTarget().getMemory();
                memoryMap.put(MemKeys.LOCAL, memory);
                memoryMap.remove(MemKeys.ENTITY);
                memoryMap.remove(MemKeys.PERSON_FACTION);
            }
        }

        return true;
    }
}

