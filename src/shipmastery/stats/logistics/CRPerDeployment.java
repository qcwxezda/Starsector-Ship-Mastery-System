package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class CRPerDeployment extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getCRPerDeploymentPercent();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(
                spec.getCRToDeploy(), 0.05f, 0.15f, 0.5f);
    }
}
