package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class PseudocoreUplinkPlugin extends BaseSpecialItemPlugin {

    public static final float MAX_AUTOCONSTRUCT_PTS_PER_UPLINK = 180f;
    public static final float PTS_MULT_MK2 = 2f;
    public static final String IS_UPLINK_TAG = "sms_uplink";
    public static final String IS_MK2_MEM_KEY = "$sms_SelectedUplinkIsMk2";

    public record PseudocoreUplinkData(float numPoints, float maxPoints, float crPenalty) {}

    public static boolean isMk2(SpecialItemSpecAPI spec) {
        return spec != null && "sms_pseudocore_uplink_mk2".equals(spec.getId());
    }

    public static PseudocoreUplinkData getPseudocoreCRPointsAndPenalty() {
        if (Global.getSector().getPlayerFleet() == null) return new PseudocoreUplinkData(0f, 0f,0f); // Not finished loading save yet...
        var fleetData = Global.getSector().getPlayerFleet().getFleetData();

        float maxPoints = (float) Global.getSector().getPlayerFleet().getCargo().getStacksCopy()
                .stream()
                .filter(stack ->
                        stack.isSpecialStack() && stack.getSpecialItemSpecIfSpecial().hasTag(IS_UPLINK_TAG))
                .mapToDouble(stack ->
                        stack.getSize() * MAX_AUTOCONSTRUCT_PTS_PER_UPLINK * (isMk2(stack.getSpecialItemSpecIfSpecial()) ? PTS_MULT_MK2 : 1f))
                .sum();

        float autoconstuctPoints = 0f;
        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleetData)) {
            if (fm.getCaptain() == null || !fm.getCaptain().isAICore()) continue;
            if (Misc.isAutomated(fm)) continue;
            if (!fm.getVariant().hasHullMod("sms_pseudocore_uplink_handler")) continue;
            autoconstuctPoints += fm.getCaptain().getMemoryWithoutUpdate().getFloat("$autoPointsMult") * fm.getDeploymentPointsCost();
        }

        // resolve NaN with no max points
        autoconstuctPoints = Math.max(autoconstuctPoints, Float.MIN_VALUE);
        maxPoints = Math.max(maxPoints, Float.MIN_VALUE);
        float crPenalty = Math.max(0f, 1f - maxPoints / autoconstuctPoints);
        return new PseudocoreUplinkData(autoconstuctPoints, maxPoints, crPenalty);
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("sms_tPseudocoreUplinkClicked");
        plugin.setCustom1(helper);
        var target = Global.getSector().getPlayerFleet();
        target.getMemoryWithoutUpdate().set(IS_MK2_MEM_KEY, isMk2(getSpec()), 0f);
        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(plugin, target, () -> {});
    }

    @Override
    public boolean hasRightClickAction() {
        return true;
    }

    @Override
    public boolean shouldRemoveOnRightClickAction() {
        return false;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource, false);
        var data = getPseudocoreCRPointsAndPenalty();
        tooltip.addPara(Strings.Items.uplinkDesc, 10f, Misc.getHighlightColor(), Utils.asInt(data.maxPoints));
        if (data.crPenalty <= 0f) {
            tooltip.addPara(Strings.Items.uplinkStatus, 10f, Misc.getHighlightColor(), Utils.asInt(data.numPoints));
        } else {
            tooltip.addPara(Strings.Items.uplinkStatus2, 10f, new Color[] {Misc.getHighlightColor(), Misc.getNegativeHighlightColor()}, Utils.asInt(data.numPoints), Utils.asPercent(data.crPenalty));
        }
        addCostLabel(tooltip, 10f, transferHandler, stackSource);
        if (isMk2(getSpec())) {
            tooltip.addPara(Strings.Items.uplinkMk2RightClick, Misc.getPositiveHighlightColor(), 10f);
        } else {
            tooltip.addPara(Strings.Items.uplinkRightClick, Misc.getPositiveHighlightColor(), 10f);
        }
    }
}
