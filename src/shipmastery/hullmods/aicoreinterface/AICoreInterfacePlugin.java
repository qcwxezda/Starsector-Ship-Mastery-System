package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;

public interface AICoreInterfacePlugin {
    String INTEGRATED_SUFFIX = "<sms_interface>";
    float getIntegrationCost(FleetMemberAPI member);
    default void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Misc.noEffect, 10f);
    }
    default String getCannotIntegrateReason(FleetMemberAPI member) {
        return null;
    }
    default String getCannotRemoveReason(FleetMemberAPI member) {
        return null;
    }

    static String getIntegratedPseudocore(ShipVariantAPI variant) {
        return variant.getHullMods()
                .stream()
                .<String>mapMulti((id, consumer) -> {
                    var spec = Global.getSettings().getHullModSpec(id);
                    if (spec.getEffect() instanceof AICoreInterfaceHullmod p) {
                        consumer.accept(p.getItemId());
                    }
                })
                .findFirst()
                .orElse(null);
    }

    static void addIntegratedDescToTooltip(TooltipMakerAPI tooltip, String coreId, float pad) {
        tooltip.addPara(Strings.MasteryPanel.aiIntegratedTooltip, pad, Settings.POSITIVE_HIGHLIGHT_COLOR, Global.getSettings().getCommoditySpec(coreId).getName());
        tooltip.addSpacer(10f);
        var plugin = (AICoreInterfacePlugin) Global.getSettings().getHullModSpec(coreId + AICoreInterfacePlugin.INTEGRATED_SUFFIX).getEffect();
        plugin.addIntegrationDescriptionToTooltip(tooltip);
    }
}
