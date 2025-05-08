package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Objects;

public class KCoreUplinkPlugin extends BaseSpecialItemPlugin {

    public static final float MAX_AUTOCONSTRUCT_PTS_PER_UPLINK = 180f;
    public static final String IS_AUTOCONSTRUCT_TAG = "sms_k_core";
    public static final String AUTOCONSTRUCT_PENALTY_CACHE = "sms_KCorePenalty";

    public record KCoreUplinkData(int numUplinks, float numPoints, float crPenalty) {}

    public static KCoreUplinkData getKCoreCRPointsAndPenalty() {
        if (Global.getSector().getPlayerFleet() == null) return new KCoreUplinkData(0, 0f, 0f); // Not finished loading save yet...
        var fleetData = Global.getSector().getPlayerFleet().getFleetData();
        var data = (KCoreUplinkData) fleetData.getCacheClearedOnSync().get(AUTOCONSTRUCT_PENALTY_CACHE);
        if (data != null) return data;

        int uplinkCount = Global.getSector().getPlayerFleet().getCargo().getStacksCopy()
                .stream()
                .filter(stack -> stack.isSpecialStack() && "sms_k_core_uplink".equals(stack.getSpecialItemSpecIfSpecial().getId()))
                .mapToInt(stack -> (int) stack.getSize())
                .sum();

        float maxPoints = MAX_AUTOCONSTRUCT_PTS_PER_UPLINK * uplinkCount;

        float autoconstuctPoints = 0f;
        for (FleetMemberAPI fm : Utils.getMembersNoSync(fleetData)) {
            if (fm.getCaptain() == null || !fm.getCaptain().isAICore()) continue;
            if (Misc.isAutomated(fm)) continue;
            var fmCore = Global.getSettings().getCommoditySpec(fm.getCaptain().getAICoreId());
            if (fmCore == null || !fmCore.hasTag(IS_AUTOCONSTRUCT_TAG)) continue;
            autoconstuctPoints += fm.getCaptain().getMemoryWithoutUpdate().getFloat("$autoPointsMult") * fm.getDeploymentPointsCost();
        }

        float crPenalty = Math.max(0f, 1f - maxPoints / autoconstuctPoints);
        data = new KCoreUplinkData(uplinkCount, autoconstuctPoints, crPenalty);
        fleetData.getCacheClearedOnSync().put(AUTOCONSTRUCT_PENALTY_CACHE, data);
        return data;
    }

    public static void applyKCoreCRPenalty(MutableShipStatsAPI stats, String id) {
        if (Global.getSector().getPlayerFleet() == null) return; // Not finished loading the save yet...
        if (stats.getFleetMember() == null || stats.getFleetMember().getCaptain() == null) return;
        if (Misc.isAutomated(stats.getFleetMember())) return;
        var fleetData = stats.getFleetMember().getFleetData();
        if (!Objects.equals(fleetData, Global.getSector().getPlayerFleet().getFleetData())) return;

        var captain = stats.getFleetMember().getCaptain();
        if (!captain.isAICore()) return;
        var core = Global.getSettings().getCommoditySpec(captain.getAICoreId());
        if (core == null || !core.hasTag(IS_AUTOCONSTRUCT_TAG)) return;

        float crPenalty = getKCoreCRPointsAndPenalty().crPenalty;
        if (crPenalty > 0f) {
            stats.getMaxCombatReadiness().modifyFlat(id, -crPenalty, Strings.Items.uplinkPenaltyDesc);
        }
    }

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("sms_tKCoreUplinkClicked");
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
        var data = getKCoreCRPointsAndPenalty();
        tooltip.addPara(Strings.Items.uplinkDesc, 10f, Misc.getHighlightColor(), Utils.asInt(MAX_AUTOCONSTRUCT_PTS_PER_UPLINK*data.numUplinks));
        if (data.crPenalty <= 0f) {
            tooltip.addPara(Strings.Items.uplinkStatus, 10f, Misc.getHighlightColor(), Utils.asInt(data.numPoints));
        } else {
            tooltip.addPara(Strings.Items.uplinkStatus2, 10f, new Color[] {Misc.getHighlightColor(), Misc.getNegativeHighlightColor()}, Utils.asInt(data.numPoints), Utils.asPercent(data.crPenalty));
        }
        this.addCostLabel(tooltip, 10f, transferHandler, stackSource);
        tooltip.addPara(Strings.Items.uplinkRightClick, Misc.getPositiveHighlightColor(), 10f);
    }
}
