package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
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

    public record PseudocoreUplinkData(int numUplinks, float numPoints, float crPenalty) {}

    public static PseudocoreUplinkData getPseudocoreCRPointsAndPenalty() {
        if (Global.getSector().getPlayerFleet() == null) return new PseudocoreUplinkData(0, 0f, 0f); // Not finished loading save yet...
        var fleetData = Global.getSector().getPlayerFleet().getFleetData();

        int uplinkCount = Global.getSector().getPlayerFleet().getCargo().getStacksCopy()
                .stream()
                .filter(stack -> stack.isSpecialStack() && "sms_pseudocore_uplink".equals(stack.getSpecialItemSpecIfSpecial().getId()))
                .mapToInt(stack -> (int) stack.getSize())
                .sum();

        float maxPoints = MAX_AUTOCONSTRUCT_PTS_PER_UPLINK * uplinkCount;

        float autoconstuctPoints = 0f;
        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleetData)) {
            if (fm.getCaptain() == null || !fm.getCaptain().isAICore()) continue;
            if (Misc.isAutomated(fm)) continue;
            var fmCore = Global.getSettings().getCommoditySpec(fm.getCaptain().getAICoreId());
            if (fmCore == null || !fmCore.hasTag(BasePseudocorePlugin.IS_PSEUDOCORE_TAG)) continue;
            autoconstuctPoints += fm.getCaptain().getMemoryWithoutUpdate().getFloat("$autoPointsMult") * fm.getDeploymentPointsCost();
        }

        // resolve NaN with no max points
        autoconstuctPoints = Math.max(autoconstuctPoints, Float.MIN_VALUE);
        maxPoints = Math.max(maxPoints, Float.MIN_VALUE);
        float crPenalty = Math.max(0f, 1f - maxPoints / autoconstuctPoints);
        return new PseudocoreUplinkData(uplinkCount, autoconstuctPoints, crPenalty);
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("sms_tPseudocoreUplinkClicked");
        plugin.setCustom1(helper);
        Global.getSector().getCampaignUI().showInteractionDialogFromCargo(plugin, Global.getSector().getPlayerFleet(), () -> {});
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
        tooltip.addPara(Strings.Items.uplinkDesc, 10f, Misc.getHighlightColor(), Utils.asInt(MAX_AUTOCONSTRUCT_PTS_PER_UPLINK*data.numUplinks));
        if (data.crPenalty <= 0f) {
            tooltip.addPara(Strings.Items.uplinkStatus, 10f, Misc.getHighlightColor(), Utils.asInt(data.numPoints));
        } else {
            tooltip.addPara(Strings.Items.uplinkStatus2, 10f, new Color[] {Misc.getHighlightColor(), Misc.getNegativeHighlightColor()}, Utils.asInt(data.numPoints), Utils.asPercent(data.crPenalty));
        }
        addCostLabel(tooltip, 10f, transferHandler, stackSource);
        tooltip.addPara(Strings.Items.uplinkRightClick, Misc.getPositiveHighlightColor(), 10f);
    }
}
