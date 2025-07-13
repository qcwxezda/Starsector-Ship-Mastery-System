package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.util.MathUtils;

public abstract class AICoreInterfaceHullmod extends BaseHullMod implements AICoreInterfacePlugin {
    public final CargoStackAPI getRequiredItem() {
        return Global.getSettings().createCargoStack(
                CargoAPI.CargoItemType.RESOURCES,
                getItemId(),
                null);
    }

    public final float getDefaultIntegrationCost(FleetMemberAPI member, float minCost, float maxCost) {
        float cost = MathUtils.lerp(minCost, maxCost, MathUtils.clamp(member.getUnmodifiedDeploymentPointsCost() / 60f, 0f, 1f));
        cost = 1000f * (int) (cost/1000f);
        return cost;
    }

    public final String getItemId() {
        if (spec == null) return "";
        String id = spec.getId();
        return id.substring(0, id.lastIndexOf(INTEGRATED_SUFFIX));
    }
}
