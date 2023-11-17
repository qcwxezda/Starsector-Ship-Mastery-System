package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class CRPerDeployment extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getCRPerDeploymentPercent();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return 0f;
        return spec.getCRToDeploy();
    }
}
