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
import shipmastery.campaign.items.SuperconstructPlugin;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_ConcealedStationInteraction extends BaseCommandPlugin {
    public static final String SMS_USED_KEY = "$sms_ConcealedStationUsed";
    public static final String SMS_SHIP_NAME_KEY = "$sms_ShipName";
    public static final String SMS_COLOR_KEY = "$sms_TextColor";

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Misc.Token> params,
                           final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        List<FleetMemberAPI> selectable = new ArrayList<>();
        for (FleetMemberAPI fm : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (!fm.getVariant().hasHullMod("sms_extradimensional_rearrangement1")) {
                selectable.add(fm);
            }
        }
        dialog.showFleetMemberPickerDialog(
                Strings.Misc.selectAShip,
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                4,
                6,
                80f,
                true,
                false,
                selectable,
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
                        picked.getVariant().addPermaMod("sms_extradimensional_rearrangement5");

                        picked.getRepairTracker().setCR(0);
                        memoryMap.get(MemKeys.LOCAL).set(SMS_USED_KEY, true);
                        memoryMap.get(MemKeys.LOCAL).set(SMS_SHIP_NAME_KEY, picked.getShipName());
                        memoryMap.get(MemKeys.LOCAL).set(SMS_COLOR_KEY, Misc.getNegativeHighlightColor());
                        if (dialog.getInteractionTarget() != null) {
                            CargoAPI cargo = Global.getFactory().createCargo(true);
                            cargo.addSpecial(
                                    new SpecialItemData("sms_construct", Utils.getRestoredHullSpecId(picked.getHullSpec())),
                                    3 + Misc.random.nextInt(3)); // 3-5
                            cargo.addSpecial(new SpecialItemData("sms_superconstruct1", SuperconstructPlugin.ACTIVE_STRING), 1f);
                            cargo.addSpecial(new SpecialItemData("sms_superconstruct2", SuperconstructPlugin.ACTIVE_STRING), 1f);
                            BaseSalvageSpecial.addExtraSalvage(dialog.getInteractionTarget(), cargo);
                        }
                        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
                        Global.getSoundPlayer().pauseMusic();
                        DeferredActionPlugin.performOnUnpause(() -> Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false));
                        FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationDocked");
                    }
                });
        return true;
    }
}
