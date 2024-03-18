package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_ConcealedStationInteraction extends BaseCommandPlugin {
    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Misc.Token> params,
                           final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        dialog.showFleetMemberPickerDialog(
                Strings.SELECT_A_SHIP,
                Strings.CONFIRM_STR,
                Strings.CANCEL_STR,
                4,
                6,
                80f,
                true,
                false,
                Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy(),
                new FleetMemberPickerListener() {
                    @Override
                    public void cancelledFleetMemberPicking() {
                    }
                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> fleetMembers) {
                        if (fleetMembers == null || fleetMembers.isEmpty()) {
                            return;
                        }
                        FleetMemberAPI picked = fleetMembers.get(0);
                        picked.getVariant().addPermaMod("sms_randomOverloadVenting");
                        picked.getRepairTracker().setCR(0);
                        memoryMap.get(MemKeys.LOCAL).set("$sms_ConcealedStationUsed", true);
                        if (dialog.getInteractionTarget() != null) {
                            CargoAPI cargo = Global.getFactory().createCargo(true);
                            cargo.addSpecial(
                                    new SpecialItemData("sms_construct", Utils.getRestoredHullSpecId(picked.getHullSpec())),
                                    3 + Misc.random.nextInt(3)); // 3-5
                            BaseSalvageSpecial.addExtraSalvage(dialog.getInteractionTarget(), cargo);
                        }
                        FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationDocked");
                    }
                });
        return true;
    }
}
