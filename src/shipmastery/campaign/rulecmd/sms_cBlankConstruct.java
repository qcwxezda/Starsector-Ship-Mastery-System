package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import shipmastery.ShipMastery;
import shipmastery.campaign.MasterySharingHandler;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.campaign.items.SuperconstructPlugin;
import shipmastery.config.Settings;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.CampaignUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cBlankConstruct extends BaseCommandPlugin {

    public static final String SUPERCONSTRUCT_MODIFIER_ID = "sms_Superconstruct1";
    public static final String TYPE_MEMORY_KEY = "$sms_BlankConstructType";

    public enum BlankConstructType {
        SUPERCONSTRUCT_1,
        MASTERY_SHARING_CONSTRUCT
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        try {
            ((List<?>) ReflectionUtils.uiPanelGetChildrenNonCopy.invoke(dialog)).clear();
        } catch (Throwable e) {
            Logger.getLogger(sms_cBlankConstruct.class).error("Couldn't clear dialog panel's children", e);
        }
        RuleBasedInteractionDialogPluginImpl plugin = (RuleBasedInteractionDialogPluginImpl) dialog.getPlugin();
        SpecialItemPlugin.RightClickActionHelper helper = (SpecialItemPlugin.RightClickActionHelper) plugin.getCustom1();
        dialog.showFleetMemberPickerDialog(
                "Select a ship",
                "Confirm",
                "Cancel",
                4,
                6,
                80f,
                true,
                false,
                Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy(),
                new FleetMemberPickerListener() {

                    private void addMPFromBlankConstructAndDismiss(ShipHullSpecAPI spec, float mpAmount, int numItemsUsed) {
                        ShipMastery.addPlayerMasteryPoints(spec, mpAmount, false, false, ShipMastery.MasteryGainSource.ITEM);
                        var messageDisplay = Global.getSector().getCampaignUI().getMessageDisplay();
                        messageDisplay.addMessage(String.format(Strings.Messages.gainedMPSingle, Utils.asInt(mpAmount) + " " + Strings.Misc.XP, spec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);
                        Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
                        helper.removeFromClickedStackFirst(numItemsUsed);
                        dialog.dismiss();
                    }

                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> members) {
                        if (members == null || members.isEmpty()) {
                            dialog.dismissAsCancel();
                            return;
                        }
                        FleetMemberAPI member = members.get(0);
                        ShipHullSpecAPI spec = Utils.getRestoredHullSpec(member.getHullSpec());
                        var type = (BlankConstructType) getEntityMemory(memoryMap).get(TYPE_MEMORY_KEY);
                        switch (type) {
                            case SUPERCONSTRUCT_1 -> {
                                Global.getSector().getPlayerStats().getDynamic().getMod(MasteryEffect.MASTERY_STRENGTH_MOD_FOR + spec.getHullId()).modifyPercent(SUPERCONSTRUCT_MODIFIER_ID, 100f * SuperconstructPlugin.SUPERCONSTRUCT1_STRENGTH);
                                member.getVariant().addPermaMod("sms_superconstruct_hullmod", false);
                                ShipMastery.addPlayerMasteryPoints(spec, SuperconstructPlugin.SUPERCONSTRUCT1_MP, false, false, ShipMastery.MasteryGainSource.ITEM);
                                var messageDisplay = Global.getSector().getCampaignUI().getMessageDisplay();
                                messageDisplay.addMessage(String.format(Strings.Messages.gainedMPSingle, SuperconstructPlugin.SUPERCONSTRUCT1_MP + " " + Strings.Misc.XP, spec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);
                                messageDisplay.addMessage(String.format(Strings.Items.superconstruct1MessageDisplay1, Utils.asPercent(SuperconstructPlugin.SUPERCONSTRUCT1_STRENGTH)), Settings.MASTERY_COLOR);
                                messageDisplay.addMessage(String.format(Strings.Items.superconstruct1MessageDisplay2, "" + SuperconstructPlugin.SUPERCONSTRUCT1_SMODS), Settings.MASTERY_COLOR);
                                Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
                                helper.removeFromClickedStackFirst(1);
                                dialog.dismiss();
                            }
                            case MASTERY_SHARING_CONSTRUCT -> {
                                int numInCargo = (int) helper.getNumItems(CargoAPI.CargoItemType.SPECIAL, new SpecialItemData("sms_construct", KnowledgeConstructPlugin.PLAYER_CREATED_PREFIX));
                                if (numInCargo <= 0) return;
                                if (numInCargo == 1) {
                                    addMPFromBlankConstructAndDismiss(spec, MasterySharingHandler.SHARED_MASTERY_MP_GAIN, 1);
                                    return;
                                }
                                DeferredActionPlugin.performLater(() -> dialog.showCustomDialog(450f, 110f, new CampaignUtils.TextFieldDelegate(Strings.Items.selectQuantity, "1") {
                                    @Override
                                    public void onConfirm(String text) {
                                        try {
                                            int amount = Integer.parseInt(text);
                                            if (amount <= 0) {
                                                error();
                                                return;
                                            }
                                            amount = Math.min(amount, numInCargo);
                                            var pts = amount * MasterySharingHandler.SHARED_MASTERY_MP_GAIN;
                                            addMPFromBlankConstructAndDismiss(spec, amount * MasterySharingHandler.SHARED_MASTERY_MP_GAIN, amount);
                                        } catch (NumberFormatException e) {
                                            error();
                                        }
                                    }

                                    @Override
                                    public void customDialogCancel() {
                                        dialog.dismissAsCancel();
                                    }

                                    private void error() {
                                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.Items.invalidQuantity, Misc.getNegativeHighlightColor());
                                        dialog.dismissAsCancel();
                                    }
                                }), 0f);
                            }
                        }
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        dialog.dismissAsCancel();
                    }
                });
        return true;
    }
}
