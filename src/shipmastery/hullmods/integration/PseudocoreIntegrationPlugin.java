package shipmastery.hullmods.integration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public interface PseudocoreIntegrationPlugin {
    String INTEGRATED_SUFFIX = "<integrated>";
    void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip);
    float getIntegrationCost(FleetMemberAPI member);
    static String getIntegratedPseudocore(ShipVariantAPI variant) {
        return variant.getHullMods()
                .stream()
                .<String>mapMulti((id, consumer) -> {
                    var spec = Global.getSettings().getHullModSpec(id);
                    if (spec.getEffect() instanceof PseudocoreIntegrationHullmod p) {
                        consumer.accept(p.getItemId());
                    }
                })
                .findFirst()
                .orElse(null);
    }
}
