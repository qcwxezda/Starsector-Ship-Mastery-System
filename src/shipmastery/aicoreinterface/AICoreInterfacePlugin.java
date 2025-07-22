package shipmastery.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;

public interface AICoreInterfacePlugin {
    String INTEGRATED_SUFFIX = "<sms_interface>";

    default float getIntegrationCost(FleetMemberAPI member) {
        return 0f;
    }

    default void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Misc.noEffect, 0f);
    }

    default String getCannotIntegrateReason(FleetMemberAPI member) {
        return null;
    }

    default String getCannotRemoveReason(FleetMemberAPI member) {
        return null;
    }

    default void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {}

    default void applyEffectsAfterShipCreation(ShipAPI ship, String id) {}

    @SuppressWarnings("unused")
    default void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {}

    static String getIntegratedPseudocore(ShipVariantAPI variant) {
        for (String id : ShipMastery.getAICoreInterfaceSingletonMap().keySet()) {
            if (variant.hasTag(id + INTEGRATED_SUFFIX)) {
                return id;
            }
        }
        return null;
    }

    static float getDefaultIntegrationCost(FleetMemberAPI member, float minCost, float maxCost) {
        float cost = MathUtils.lerp(minCost, maxCost, MathUtils.clamp(member.getUnmodifiedDeploymentPointsCost() / 60f, 0f, 1f));
        cost = 1000f * (int) (cost/1000f);
        return cost;
    }

    static void addIntegratedDescToTooltip(TooltipMakerAPI tooltip, String coreId, float pad) {
        tooltip.addPara(Strings.MasteryPanel.aiIntegratedTooltip, pad, Settings.POSITIVE_HIGHLIGHT_COLOR, Global.getSettings().getCommoditySpec(coreId).getName());
        tooltip.addSpacer(10f);
        var plugin = ShipMastery.getAICoreInterfacePlugin(coreId);
        if (plugin == null) return;
        plugin.addIntegrationDescriptionToTooltip(tooltip);
    }
}
