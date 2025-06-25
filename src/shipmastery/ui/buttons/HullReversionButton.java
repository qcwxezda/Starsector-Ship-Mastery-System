package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Collection;

public class HullReversionButton extends ButtonForHullmodSelection {

    public HullReversionButton(boolean useStoryColors, ShipAPI selectedShip) {
        super(useStoryColors ? "graphics/icons/ui/sms_remove_smod_icon_green.png": "graphics/icons/ui/sms_remove_smod_icon.png", useStoryColors, selectedShip);
    }

    @Override
    protected String getTitle() {
        return Strings.MasteryPanel.removeSModsButton;
    }

    @Override
    protected String getDescriptionFormat() {
        return Strings.MasteryPanel.removeSModsPanelText;
    }

    @Override
    protected void onConfirm() {
        float cost = getModifiedCost();
        var variant = selectedShip.getVariant();
        selectedIds.forEach(variant::removePermaMod);
        Utils.getPlayerCredits().subtract(cost);
        if (isStoryOption) {
            Global.getSector().getPlayerStats().spendStoryPoints(
                    1,
                    true,
                    null,
                    true,
                    HullmodUtils.getBonusXPFraction(cost),
                    String.format(
                            Strings.MasteryPanel.removeSModsPanelUsedSPText,
                            selectedIds.size(),
                            selectedShip.getName(),
                            selectedShip.getHullSpec().getNameWithDesignationWithDashClass()));
        }
    }

    @Override
    protected String[] getDescriptionArgs() {
        return new String[] {Misc.getDGSCredits(getModifiedCost())};
    }

    @Override
    protected Collection<String> getEligibleHullmodIds() {
        return selectedShip.getVariant().getSMods();
    }

    @Override
    protected Color getButtonTextColor() {
        return Misc.getStoryOptionColor();
    }

    @Override
    protected float getBaseCost() {
        return (float) (selectedIds.stream().map(DModManager::getMod).mapToDouble(x -> HullmodUtils.getBuildInCost(x, selectedShip)).sum() * HullmodUtils.SMOD_REMOVAL_COST_MULT);
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.removeSModsButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        float frac = isStoryOption ? HullmodUtils.CREDITS_COST_MULT_SP : 1f;
        tooltip.addPara(Strings.MasteryPanel.removeSModsTooltip, 10f,
                isStoryOption ? Misc.getStoryBrightColor() : Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(HullmodUtils.SMOD_REMOVAL_COST_MULT * frac));
    }
}
