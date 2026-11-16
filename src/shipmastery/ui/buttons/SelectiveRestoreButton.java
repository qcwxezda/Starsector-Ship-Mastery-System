package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
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

public class SelectiveRestoreButton extends ButtonWithHullmodSelection {

    protected FleetMemberAPI member;

    public SelectiveRestoreButton(boolean useStoryColors, FleetMemberAPI member, ShipAPI selectedShip) {
        super(useStoryColors ? "graphics/icons/ui/sms_restore_icon_green.png": "graphics/icons/ui/sms_restore_icon.png", useStoryColors, selectedShip);
        this.member = member;
    }

    @Override
    protected String getTitle() {
        return Strings.MasteryPanel.selectiveRestorationButton;
    }

    @Override
    protected String getCostDescriptionFormat() {
        return Strings.MasteryPanel.selectiveRestorationPanelText;
    }

    @Override
    protected float getBxpFraction() {
        return HullmodUtils.getBonusXPFraction(getBaseCost() * Global.getSector().getEconomy().getCommoditySpec(Commodities.PARTS).getBasePrice());
    }

    @Override
    protected float getCurrencyAmount() {
        return Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(Commodities.PARTS);
    }

    @Override
    protected void applyCosts() {
        var cost = getModifiedCost();
        Global.getSector().getPlayerFleet().getCargo().removeCommodity(Commodities.PARTS, cost);
        spendSPIfNeededAndPlaySound();
        ListenerUtil.reportDModsRepaired(member, selectedShip.getVariant(), cost);
    }

    @Override
    protected void applyEffects() {
        var baseSpec = Utils.getRestoredHullSpec(selectedShip.getHullSpec());
        var variant = selectedShip.getVariant();
        selectedItems.forEach(x -> {
            String id = x.getId();
            variant.removePermaMod(id);
            if (baseSpec.isBuiltInMod(id)) {
                variant.addSuppressedMod(id);
            }
        });
        if (DModManager.getNumDMods(variant) <= 0) {
            variant.setHullSpecAPI(baseSpec);
        }
    }

    @Override
    protected String getUsedSPDescription() {
        return String.format(
                Strings.MasteryPanel.selectiveRestorationUsedSPText,
                selectedItems.size(),
                selectedShip.getName(),
                selectedShip.getHullSpec().getNameWithDesignationWithDashClass());
    }

    @Override
    protected String[] getCostDescriptionArgs() {
        int cost = getModifiedCost();
        return new String[] {Integer.toString(cost),
                cost == 1 ? Strings.MasteryPanel.domainGradePartsSingle : Strings.MasteryPanel.domainGradePartsPlural};
    }

    @Override
    protected Collection<Item<HullModSpecAPI>> getEligibleItems() {
        return selectedShip.getVariant().getHullMods()
                .stream()
                .map(DModManager::getMod)
                .filter(x -> x.hasTag(Tags.HULLMOD_DMOD))
                .sorted(Comparator.comparing(HullModSpecAPI::getDisplayName))
                .map(HullmodItem::new)
                .collect(Collectors.toList());
    }

    @Override
    protected Color getButtonTextColor() {
        return Misc.getNegativeHighlightColor();
    }

    @Override
    protected int getBaseCost() {
        int toRestore = selectedItems.size();
        int base = selectedShip.getHullSpec().getPartsPerDmod() * toRestore;
        float discount = Math.min(HullmodUtils.SELECTIVE_RESTORE_DISCOUNT_MAX, HullmodUtils.SELECTIVE_RESTORE_DISCOUNT_PER * toRestore);
        return toRestore == 0 ? 0 : (int) Math.ceil(base * (1f - discount));
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.selectiveRestorationButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        float frac = isStoryOption ? HullmodUtils.getCostMultSP() : 1f;
        float maxPrice = 1f - HullmodUtils.SELECTIVE_RESTORE_DISCOUNT_PER;
        float minPrice = 1f - HullmodUtils.SELECTIVE_RESTORE_DISCOUNT_MAX;
        tooltip.addPara(Strings.MasteryPanel.selectiveRestorationTooltip,
                10f,
                isStoryOption ? Misc.getStoryBrightColor() : Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(1f - maxPrice * frac),
                Utils.asPercent(1f - minPrice * frac));
    }
}
