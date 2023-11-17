package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.ReflectionUtils;

public class CRRecoveryRate extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getBaseCRRecoveryRatePercentPerDay();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return 0f;
        // For some reason repair % per day isn't exposed in API ???
        float repairPercent = (float) ReflectionUtils.invokeMethod(spec, "getRepairPercentPerDay");
        return Math.max(1f, 20f - repairPercent);
    }
}
