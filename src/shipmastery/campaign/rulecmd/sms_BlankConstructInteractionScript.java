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
import shipmastery.ShipMastery;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_BlankConstructInteractionScript extends BaseCommandPlugin {

    public static final String MODIFIER_ID = "sms_BlankConstruct";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        dialog.setPromptText("");
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
                    dialog.dismiss();
                    return;
                }
                FleetMemberAPI member = members.get(0);
                ShipHullSpecAPI spec = Utils.getRestoredHullSpec(member.getHullSpec());
                ShipMastery.addPlayerMasteryPoints(spec, KnowledgeConstructPlugin.SUPERCONSTRUCT_MP, false, false);
                Global.getSector().getPlayerStats().getDynamic().getMod(MasteryEffect.MASTERY_STRENGTH_MOD_FOR + spec.getHullId()).modifyPercent(MODIFIER_ID, 100f*KnowledgeConstructPlugin.SUPERCONSTRUCT_STRENGTH);
                member.getVariant().addPermaMod("sms_superconstructHullmod", false);
                var messageDisplay = Global.getSector().getCampaignUI().getMessageDisplay();
                messageDisplay.addMessage(String.format(Strings.Messages.gainedMPSingle, KnowledgeConstructPlugin.SUPERCONSTRUCT_MP + " MP", spec.getHullNameWithDashClass()), Settings.MASTERY_COLOR);
                messageDisplay.addMessage(String.format(Strings.Items.superconstructMessageDisplay1, Utils.asPercent(KnowledgeConstructPlugin.SUPERCONSTRUCT_STRENGTH)), Settings.MASTERY_COLOR);
                messageDisplay.addMessage(String.format(Strings.Items.superconstructMessageDisplay2, "" + KnowledgeConstructPlugin.SUPERCONSTRUCT_SMODS), Settings.MASTERY_COLOR);
                Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
                helper.removeFromClickedStackFirst(1);
                dialog.dismiss();
            }

            @Override
            public void cancelledFleetMemberPicking() {
                dialog.dismiss();
            }
        });
        return true;
    }
}
