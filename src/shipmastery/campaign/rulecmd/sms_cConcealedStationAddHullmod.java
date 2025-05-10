package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.util.Misc;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Map;
import java.util.Random;

@SuppressWarnings("unused")
public class sms_cConcealedStationAddHullmod extends BaseCommandPlugin {
    public static final String SMS_SHIP_NAME = "$sms_ShipName";

    @Override
    public boolean execute(final String ruleId, final InteractionDialogAPI dialog, final List<Misc.Token> params,
                           final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        var memory = getEntityMemory(memoryMap);
        int num = memory.getInt(Strings.Campaign.HULLMOD_NUM_TO_ADD);
        var members = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
        // Since automating a ship will remove the human captain, an edge case may occur if said captain is the player
        // and there's no other non-automated ship for the game to automatically move the player to.
        // In this case we must prevent the flagship from being chosen
        boolean canPickFlagshipForAutomation;
        int nonAutomatedCount = (int) members.stream().filter(fm -> !Misc.isAutomated(fm)).count();
        canPickFlagshipForAutomation = nonAutomatedCount > 1;
        List<FleetMemberAPI> selectable = members.stream().filter(fm -> {
                    var variant = fm.getVariant();
                    if (variant.hasHullMod(Strings.Hullmods.REARRANGEMENT1)) return false;
                    if (variant.hasHullMod(Strings.Hullmods.REARRANGEMENT2)) return false;
                    if (variant.hasHullMod(Strings.Hullmods.REARRANGEMENT3)) return false;
                    if (variant.hasHullMod(Strings.Hullmods.REARRANGEMENT4)) return false;
                    if (num == 4 && !canPickFlagshipForAutomation && (fm.isFlagship() || (fm.getCaptain() != null && fm.getCaptain().isPlayer()))) return false;
                    return !variant.hasHullMod(Strings.Hullmods.REARRANGEMENT5);
                }
        ).toList();

        dialog.showFleetMemberPickerDialog(
                Strings.Misc.selectAShip,
                Strings.Misc.confirm,
                Strings.Misc.cancel,
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
                        picked.getVariant().addPermaMod("sms_extradimensional_rearrangement" + num);
                        // Rearrangement 4 (the one that automates the ship) needs most of it done here
                        if (num == 4) {
                            picked.getVariant().addTag(Tags.AUTOMATED);
                            picked.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
                            if (picked.getCaptain() != null && !picked.getCaptain().isAICore()) {
                                picked.setCaptain(null);
                                picked.setFlagship(false);
                            }
                        }

                        Random random = new Random((num + Global.getSector().getSeedString()).hashCode());
                        int dNum = 1 + random.nextInt(4);
                        picked.getVariant().addPermaMod("sms_extradimensional_rearrangement_d" + dNum);

                        picked.getRepairTracker().setCR(0);
                        memory.set(Strings.Campaign.STATION_USED_KEY, true);
                        memory.set(SMS_SHIP_NAME, picked.getShipName());
                        if (dialog.getInteractionTarget() != null) {
                            CargoAPI cargo = Global.getFactory().createCargo(true);
                            cargo.addSpecial(
                                    new SpecialItemData("sms_construct", Utils.getRestoredHullSpecId(picked.getHullSpec())),
                                    3 + Misc.random.nextInt(3)); // 3-5
                            BaseSalvageSpecial.addExtraSalvage(dialog.getInteractionTarget(), cargo);
                        }

                        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
                        Global.getSoundPlayer().pauseMusic();
                        DeferredActionPlugin.performOnUnpause(() -> Global.getSoundPlayer().setSuspendDefaultMusicPlayback(false));
                        FireBest.fire(null, dialog, memoryMap, "sms_tConcealedStationAddedHullmod");
                    }
                });
        return true;
    }
}
