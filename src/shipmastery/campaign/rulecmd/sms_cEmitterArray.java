package shipmastery.campaign.rulecmd;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import shipmastery.plugin.EmitterArrayPlugin;
import shipmastery.util.IntRef;
import shipmastery.util.Strings;

import java.awt.Color;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class sms_cEmitterArray extends BaseCommandPlugin {
    public static final String LEFT_SELECTOR = "sms_left", CENTER_SELECTOR = "sms_center", RIGHT_SELECTOR = "sms_right";
    public static final String CONFIRM = "sms_confirm", LEAVE = "sms_leave";
    public static final int MAX_UNITS = 4;

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getInteractionTarget() == null || !(dialog.getInteractionTarget() instanceof CustomCampaignEntityAPI entity)) return false;
        if (!(entity.getCustomPlugin() instanceof EmitterArrayPlugin emitterPlugin)) return false;

        var origPlugin = dialog.getPlugin();
        var optionPanel = dialog.getOptionPanel();
        dialog.setPromptText(String.format(Strings.Campaign.emitterPromptText, MAX_UNITS));
        optionPanel.clearOptions();
        optionPanel.addSelector(Strings.Campaign.emitterLeft, LEFT_SELECTOR, Color.WHITE, 500f, 50f, 0f, 3f, ValueDisplayMode.VALUE, Strings.Campaign.emitterLeftExt);
        optionPanel.setSelectorValue(LEFT_SELECTOR, emitterPlugin.getLeftOutput());
        optionPanel.addSelector(Strings.Campaign.emitterCenter, CENTER_SELECTOR, Color.WHITE, 500f, 50f, 0f, 3f, ValueDisplayMode.VALUE, Strings.Campaign.emitterCenterExt);
        optionPanel.setSelectorValue(CENTER_SELECTOR, emitterPlugin.getCenterOutput());
        optionPanel.addSelector(Strings.Campaign.emitterRight, RIGHT_SELECTOR, Color.WHITE, 500f, 50f, 0f, 3f, ValueDisplayMode.VALUE, Strings.Campaign.emitterRightExt);
        optionPanel.setSelectorValue(RIGHT_SELECTOR, emitterPlugin.getRightOutput());
        optionPanel.addOption(Strings.Campaign.emitterConfirm, CONFIRM);
        optionPanel.setShortcut(CONFIRM, Keyboard.KEY_G, false, false, false, false);
        optionPanel.addOption(Strings.Campaign.cancelText, LEAVE);
        optionPanel.setShortcut(LEAVE, Keyboard.KEY_ESCAPE, false, false, false, false);
        var newPlugin = getDialogPlugin(dialog, emitterPlugin, optionPanel);
        newPlugin.init(dialog);
        dialog.setPlugin(newPlugin);
        return true;
    }

    private static RuleBasedInteractionDialogPluginImpl getDialogPlugin(InteractionDialogAPI dialog, EmitterArrayPlugin emitterPlugin, OptionPanelAPI optionPanel) {
        IntRef roundL = new IntRef(0), roundC = new IntRef(0), roundR = new IntRef(0);
        var newPlugin = new RuleBasedInteractionDialogPluginImpl() {

            @Override
            public void init(InteractionDialogAPI dialog) {
                super.init(dialog);
                dialog.getOptionPanel().addOptionTooltipAppender(CONFIRM, (tooltip, hadOtherText) -> {
                    if (roundL.value + roundC.value + roundR.value > MAX_UNITS) {
                        tooltip.addPara(Strings.Campaign.emitterNotEnoughPower, 0f);
                    } else {
                        tooltip.addPara(Strings.Campaign.emitterConfirmExt, 0f);
                    }
                });
            }

            @Override
            public void optionSelected(String text, Object optionData) {
                if (LEAVE.equals(optionData)) {
                    dialog.dismissAsCancel();
                }
                if (CONFIRM.equals(optionData)) {
                    emitterPlugin.setLeftOutput(roundL.value);
                    emitterPlugin.setCenterOutput(roundC.value);
                    emitterPlugin.setRightOutput(roundR.value);
                    emitterPlugin.checkIfSolved();
                    dialog.dismiss();
                }
            }

            @Override
            public void advance(float amount) {
                roundL.value = Math.round(dialog.getOptionPanel().getSelectorValue(LEFT_SELECTOR));
                roundC.value = Math.round(dialog.getOptionPanel().getSelectorValue(CENTER_SELECTOR));
                roundR.value = Math.round(dialog.getOptionPanel().getSelectorValue(RIGHT_SELECTOR));
                optionPanel.setEnabled(CONFIRM, roundL.value + roundC.value + roundR.value <= MAX_UNITS);
            }
        };
        newPlugin.setEmbeddedMode(true);
        return newPlugin;
    }
}
