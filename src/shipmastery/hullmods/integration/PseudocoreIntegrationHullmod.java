package shipmastery.hullmods.integration;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.util.MathUtils;

public abstract class PseudocoreIntegrationHullmod extends BaseHullMod implements PseudocoreIntegrationPlugin {
    public CargoStackAPI getRequiredItem() {
        return Global.getSettings().createCargoStack(
                CargoAPI.CargoItemType.RESOURCES,
                getItemId(),
                null);
    }

    public float getMinIntegrationCost(FleetMemberAPI member) {
        return 0f;
    }
    public float getMaxIntegrationCost(FleetMemberAPI member) {
        return 0f;
    }

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        float min = getMinIntegrationCost(member);
        float max = getMaxIntegrationCost(member);
        return MathUtils.lerp(min, max, MathUtils.clamp(member.getUnmodifiedDeploymentPointsCost() / 60f, 0f, 1f));
    }

    public String getItemId() {
        if (spec == null) return "";
        String id = spec.getId();
        return id.substring(0, id.lastIndexOf(INTEGRATED_SUFFIX));
    }
}
