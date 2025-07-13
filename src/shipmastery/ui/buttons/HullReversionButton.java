package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

public class HullReversionButton extends ButtonWithHullmodSelection {

    public HullReversionButton(boolean useStoryColors, ShipAPI selectedShip) {
        super(useStoryColors ? "graphics/icons/ui/sms_remove_smod_icon_green.png": "graphics/icons/ui/sms_remove_smod_icon.png", useStoryColors, selectedShip);
    }

    @Override
    protected String getTitle() {
        return Strings.MasteryPanel.removeSModsButton;
    }

    @Override
    protected String getCostDescriptionFormat() {
        return Strings.MasteryPanel.removeSModsPanelText;
    }

    @Override
    protected void applyEffects() {
        var variant = selectedShip.getVariant();
        selectedItems.forEach(x -> variant.removePermaMod(x.getId()));
    }

    @Override
    protected String getUsedSPDescription() {
        return String.format(
                Strings.MasteryPanel.removeSModsPanelUsedSPText,
                selectedItems.size(),
                selectedShip.getName(),
                selectedShip.getHullSpec().getNameWithDesignationWithDashClass());
    }

    @Override
    protected String[] getCostDescriptionArgs() {
        return new String[] {Misc.getDGSCredits(getModifiedCost())};
    }

    @Override
    protected Collection<Item<HullModSpecAPI>> getEligibleItems() {
        return selectedShip.getVariant().getSMods().stream()
                .map(x -> Global.getSettings().getHullModSpec(x))
                .sorted(Comparator.comparing(HullModSpecAPI::getDisplayName))
                .map(HullmodItem::new)
                .collect(Collectors.toList());
    }

    @Override
    protected Color getButtonTextColor() {
        return Misc.getStoryOptionColor();
    }

    @Override
    protected float getBaseCost() {
        return (float) (selectedItems.stream().mapToDouble(x -> HullmodUtils.getBuildInCost(x.getItem(), selectedShip)).sum() * HullmodUtils.SMOD_REMOVAL_COST_MULT);
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
