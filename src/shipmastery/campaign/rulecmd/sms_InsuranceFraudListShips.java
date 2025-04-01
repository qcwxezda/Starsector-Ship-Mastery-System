package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.graveyard.InsuranceFraudDetector;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class sms_InsuranceFraudListShips extends BaseCommandPlugin {

    public static final String FORFEIT_OPTION = "sms_oInsuranceForfeit";
    public static final String ADDITIONAL_COST_KEY = "$sms_insuranceFraudAdditionalCost";

    @Override
    public boolean execute(String ruleId,  InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;

        Collection<FleetMemberAPI> fraudMembers = getShips();
        Set<String> playerMemberIds = new HashSet<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            playerMemberIds.add(member.getId());
        }

        Collection<FleetMemberAPI> noLongerInFleet = new TreeSet<>(Utils.byDPComparator);
        Map<String, Float> paidAmounts = getPaidAmounts();
        float additionalCost = 0f;

        for (FleetMemberAPI member : fraudMembers) {
            if (!playerMemberIds.contains(member.getId())) {
                noLongerInFleet.add(member);
                Float amount = paidAmounts.get(member.getId());
                if (amount != null) {
                    additionalCost += amount;
                }
            }
        }
        additionalCost = (int) additionalCost;

        dialog.getTextPanel().beginTooltip().addShipList(8, (fraudMembers.size() + 7) / 8, 50f, Misc.getBasePlayerColor(), new ArrayList<>(fraudMembers), 10f);
        dialog.getTextPanel().addTooltip();

        dialog.getTextPanel().addPara(Strings.Graveyard.insuranceListShips1);
        dialog.getTextPanel().addPara(Strings.Graveyard.insuranceListShips2);

        if (noLongerInFleet.isEmpty()) {
            dialog.getOptionPanel()
                  .addOptionConfirmation(FORFEIT_OPTION, Strings.Graveyard.insuranceForfeitConfirm,
                                         Strings.MasteryPanel.yes, Strings.MasteryPanel.no);
        }
        else {
            dialog.getTextPanel().addPara(noLongerInFleet.size() == 1 ? Strings.Graveyard.insuranceDiscrepancy1Single : Strings.Graveyard.insuranceDiscrepancy1Plural,
                                          Misc.getTextColor(),
                                          Misc.getHighlightColor(),
                                          "" + noLongerInFleet.size());
            dialog.getTextPanel().addPara(Strings.Graveyard.insuranceDiscrepancy2);
            dialog.getTextPanel().addPara(Strings.Graveyard.insuranceDiscrepancy3, Misc.getTextColor(), Misc.getHighlightColor(), Misc.getDGSCredits(additionalCost));
            memoryMap.get(MemKeys.LOCAL).set(ADDITIONAL_COST_KEY, additionalCost);
            dialog.getOptionPanel()
                  .addOptionConfirmation(FORFEIT_OPTION, String.format(Strings.Graveyard.insuranceForfeitConfirm2, Misc.getDGSCredits(additionalCost)),
                                         Strings.MasteryPanel.yes, Strings.MasteryPanel.no);
        }

        if (additionalCost > Global.getSector().getPlayerFleet().getCargo().getCredits().get()) {
            dialog.getOptionPanel().setEnabled(FORFEIT_OPTION, false);
            dialog.getOptionPanel().setTooltip(FORFEIT_OPTION, Strings.Graveyard.insuranceNotEnoughCredits);
        }

        return true;
    }

    public static Collection<FleetMemberAPI> getShips() {
        List<InsuranceFraudDetector> detectors = Global.getSector().getListenerManager().getListeners(
                InsuranceFraudDetector.class);

        Set<FleetMemberAPI> playerMembers = new TreeSet<>(Utils.byDPComparator);
        if (detectors.isEmpty()) return playerMembers;

        InsuranceFraudDetector detector = detectors.get(0);
        playerMembers.addAll(detector.getSnapshotMembers());
        return playerMembers;
    }

    public static Map<String, Float> getPaidAmounts() {
        List<InsuranceFraudDetector> detectors = Global.getSector().getListenerManager().getListeners(
                InsuranceFraudDetector.class);
        if (detectors.isEmpty()) return new HashMap<>();
        return detectors.get(0).getPaidAmounts();
    }
}

