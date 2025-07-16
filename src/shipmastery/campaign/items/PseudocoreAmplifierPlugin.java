package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.achievements.PseudocoreAmplifierIntegrated;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.util.Strings;

public class PseudocoreAmplifierPlugin extends BaseSpecialItemPlugin {
    @Override
    public boolean hasRightClickAction() {
        return true;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource, false);
        addCostLabel(tooltip, 10f, transferHandler, stackSource);
        tooltip.addPara(String.format(Strings.Items.ampRightClick, getName()), Misc.getPositiveHighlightColor(), 10f);
    }

    @Override
    public void performRightClickAction() {
        Global.getSector().getMemoryWithoutUpdate().set(Strings.Campaign.PSEUDOCORE_AMP_INTEGRATED, true);
        Global.getSoundPlayer().playUISound(getSpec().getSoundId(), 1f, 1f);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(String.format(Strings.Items.ampIntegrated, getName()));
        UnlockAchievementAction.unlockWhenUnpaused(PseudocoreAmplifierIntegrated.class);
    }
}
