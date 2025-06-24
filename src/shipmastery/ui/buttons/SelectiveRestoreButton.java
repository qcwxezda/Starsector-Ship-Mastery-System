package shipmastery.ui.buttons;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Collection;

public class SelectiveRestoreButton extends ButtonForHullmodSelection {

    public SelectiveRestoreButton(boolean useStoryColors, ShipAPI selectedShip) {
        super(useStoryColors ? "graphics/icons/ui/sms_restore_icon_green.png": "graphics/icons/ui/sms_restore_icon.png", useStoryColors, selectedShip);
    }

    @Override
    protected String getTitle() {
        return Strings.MasteryPanel.selectiveRestorationButton;
    }

    @Override
    protected String getDescriptionFormat() {
        return Strings.MasteryPanel.selectiveRestorationPanelText;
    }

    @Override
    protected void onConfirm() {
        var baseSpec = Utils.getRestoredHullSpec(selectedShip.getHullSpec());
        var variant = selectedShip.getVariant();
        selectedIds.forEach(id -> {
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
    protected String[] getDescriptionArgs() {
        return new String[] {Misc.getDGSCredits(getModifiedCost())};
    }

    @Override
    protected Collection<String> getEligibleHullmodIds() {
        return selectedShip.getVariant().getHullMods()
                .stream()
                .map(DModManager::getMod)
                .filter(x -> x.hasTag(Tags.HULLMOD_DMOD))
                .map(HullModSpecAPI::getId)
                .toList();
    }

    @Override
    protected float getBaseCost() {
        var variant = selectedShip.getVariant();
        float base = HullmodUtils.getRestorationCost(selectedShip.getVariant());
        int count = DModManager.getNumDMods(variant);
        float minMult = HullmodUtils.SELECTIVE_RESTORE_COST_MULT_MIN;
        float maxMult = HullmodUtils.SELECTIVE_RESTORE_COST_MULT_MAX;
        int toRestore = selectedIds.size();
        return toRestore == 0 ? 0f : base * (minMult + (maxMult - minMult) * (float) toRestore / count);
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.selectiveRestorationButton;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        float frac = useStoryColors ? HullmodUtils.CREDITS_COST_MULT_SP : 1f;
        tooltip.addPara(Strings.MasteryPanel.selectiveRestorationTooltip,
                10f,
                useStoryColors ? Misc.getStoryBrightColor() : Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(HullmodUtils.SELECTIVE_RESTORE_COST_MULT_MIN * frac),
                Utils.asPercent(HullmodUtils.SELECTIVE_RESTORE_COST_MULT_MAX * frac));
    }
}
