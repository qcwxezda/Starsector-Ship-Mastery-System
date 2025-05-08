package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import shipmastery.ShipMastery;
import shipmastery.campaign.items.SuperconstructPlugin;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cSuperconstruct extends BaseCommandPlugin {

    public static final String MODIFIER_ID = "sms_Superconstruct1";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        try {
            ((List<?>) ReflectionUtils.uiPanelGetChildrenNonCopy.invoke(dialog)).clear();
        } catch (Throwable e) {
            Logger.getLogger(sms_cSuperconstruct.class).error("Couldn't clear dialog panel's children", e);
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
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                if (members == null || members.isEmpty()) {
                    dialog.dismissAsCancel();
                    return;
                }
                FleetMemberAPI member = members.get(0);
                ShipHullSpecAPI spec = Utils.getRestoredHullSpec(member.getHullSpec());
                ShipMastery.addPlayerMasteryPoints(spec, SuperconstructPlugin.SUPERCONSTRUCT1_MP, false, false);
                Global.getSector().getPlayerStats().getDynamic().getMod(MasteryEffect.MASTERY_STRENGTH_MOD_FOR + spec.getHullId()).modifyPercent(MODIFIER_ID, 100f*SuperconstructPlugin.SUPERCONSTRUCT1_STRENGTH);
                member.getVariant().addPermaMod("sms_superconstruct_hullmod", false);
                var messageDisplay = Global.getSector().getCampaignUI().getMessageDisplay();
                messageDisplay.addMessage(String.format(Strings.Messages.gainedMPSingle, SuperconstructPlugin.SUPERCONSTRUCT1_MP + " MP", spec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);
                messageDisplay.addMessage(String.format(Strings.Items.superconstruct1MessageDisplay1, Utils.asPercent(SuperconstructPlugin.SUPERCONSTRUCT1_STRENGTH)), Settings.MASTERY_COLOR);
                messageDisplay.addMessage(String.format(Strings.Items.superconstruct1MessageDisplay2, "" + SuperconstructPlugin.SUPERCONSTRUCT1_SMODS), Settings.MASTERY_COLOR);
                Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
                helper.removeFromClickedStackFirst(1);
                dialog.dismiss();
            }

            @Override
            public void cancelledFleetMemberPicking() {
                dialog.dismissAsCancel();
            }
        });
        return true;
    }
}
