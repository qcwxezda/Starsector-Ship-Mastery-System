package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public class KnowledgeCoreUplinkPlugin extends BaseSpecialItemPlugin {

    @Override
    public void performRightClickAction(RightClickActionHelper helper) {
        RuleBasedInteractionDialogPluginImpl plugin = new RuleBasedInteractionDialogPluginImpl("sms_tKnowledgeCoreUplinkClicked");
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
        super.createTooltip(tooltip, expanded, transferHandler, stackSource);
    }
}
