package shipmastery.campaign.items;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public interface PseudocoreIntegrationPlugin {
    void addDescriptionToTooltip(TooltipMakerAPI tooltip);
    void applyEffect(FleetMemberAPI member);
}
