package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.rulecmd.sms_cBlankConstruct;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Objects;

public class SuperconstructPlugin extends KnowledgeConstructPlugin {
    // Basically a key; if any other string is used with a blank construct,
    // it will not work. This avoids rendering weirdness with the codex (which always passes in null)
    public static final String ACTIVE_STRING = "sms_BlankConstructActive";
    public static final int SUPERCONSTRUCT1_MP = 1000;
    public static final float SUPERCONSTRUCT1_STRENGTH = 0.25f;
    public static final int SUPERCONSTRUCT1_SMODS = 1;
    public static final float SUPERCONSTRUCT2_STRENGTH = 0.1f;
    public static final int SUPERCONSTRUCT2_NEWMPCOST = 8;
    public static final String SUPERCONSTRUCT2_MODIFIERID = "sms_Superconstruct2";

    private enum Type {
        NONE,
        TYPE_1,
        TYPE_2,
        TYPE_3
    }
    private Type type;

    @Override
    public void init(CargoStackAPI stack) {
        this.stack = stack;
        String specStr = stack.getSpecialDataIfSpecial().getData();
        if (Objects.equals(specStr, ACTIVE_STRING)) {
            type = getId().equals("sms_superconstruct1")
                    ? Type.TYPE_1 : getId().equals("sms_superconstruct2")
                    ?  Type.TYPE_2 : Type.TYPE_3;
        } else {
            type = Type.NONE;
        }
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return type == Type.TYPE_3 || type == Type.TYPE_2;
    }

    @Override
    public boolean hasRightClickAction() {
        return true;
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        Color bgColor = Global.getSector().getPlayerFaction().getDarkUIColor();
        switch (type) {
            case TYPE_1 -> bgColor = Global.getSector().getPlayerFaction().getBrightUIColor();
            case TYPE_2 -> bgColor = Utils.mixColor(new Color(1f, 0.4f, 1f, 1f), Global.getSector().getPlayerFaction().getBrightUIColor(), 0.4f);
            case TYPE_3 -> bgColor = new Color(200, 255, 225);
        }
        super.render(x, y, w, h, alphaMult, glowMult, renderer, bgColor);
    }

    @Override
    public int getPrice(MarketAPI market, SubmarketAPI submarket) {
        if (spec != null) return (int) spec.getBasePrice();
        return 0;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource);
        float opad = 10.0F;
        Color b = Misc.getPositiveHighlightColor();
        if (type == Type.TYPE_1) {
            tooltip.addPara(Strings.Items.superconstruct1RightClick, opad, b, Misc.getHighlightColor(), Utils.asInt(SUPERCONSTRUCT1_MP), Utils.asPercent(SUPERCONSTRUCT1_STRENGTH), Utils.asInt(SUPERCONSTRUCT1_SMODS));
        }
        else if (type == Type.TYPE_2) {
            tooltip.addPara(Strings.Items.superconstruct2RightClick, opad, b, Misc.getHighlightColor(), Utils.asPercent(SUPERCONSTRUCT2_STRENGTH));
        } else if (type == Type.TYPE_3) {
            tooltip.addPara(Strings.Items.superconstruct3RightClick, opad, b, Misc.getHighlightColor(), Global.getSettings().getSkillSpec("sms_shared_knowledge").getName());
        }
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        if (type == Type.TYPE_1) {
            RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("sms_tBlankConstructClicked");
            plugin.setCustom1(helper);
            var target = Global.getSector().getPlayerFleet();
            target.getMemoryWithoutUpdate().set(sms_cBlankConstruct.TYPE_MEMORY_KEY, sms_cBlankConstruct.BlankConstructType.SUPERCONSTRUCT_1, 0f);
            Global.getSector().getCampaignUI().showInteractionDialogFromCargo(plugin, target, () -> {});
        } else if (type == Type.TYPE_2) {
            Global.getSector().getPlayerStats().getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD).modifyPercent(SUPERCONSTRUCT2_MODIFIERID, 100f*SUPERCONSTRUCT2_STRENGTH);
            Global.getSector().getPersistentData().put(MasteryUtils.CONSTRUCT_MP_OVERRIDE_KEY, SUPERCONSTRUCT2_NEWMPCOST);
            var messageDisplay = Global.getSector().getCampaignUI().getMessageDisplay();
            messageDisplay.addMessage(String.format(Strings.Items.superconstruct2MessageDisplay1, Utils.asPercent(SUPERCONSTRUCT2_STRENGTH)), Settings.MASTERY_COLOR);
            Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
        } else if (type == Type.TYPE_3) {
            Global.getSector().getPlayerStats().setSkillLevel("sms_shared_knowledge", 2f);
            var messageDisplay = Global.getSector().getCampaignUI().getMessageDisplay();
            String name = Global.getSettings().getSkillSpec("sms_shared_knowledge").getName();
            messageDisplay.addMessage(String.format(Strings.Items.superconstruct3MessageDisplay1, name), Misc.getHighlightColor());
            messageDisplay.addMessage(String.format(Strings.Items.superconstruct3MessageDisplay2, name), Misc.getHighlightColor());
            Global.getSoundPlayer().playUISound("ui_neural_transfer_complete", 1, 1);
        }
    }
}