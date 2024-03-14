package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Utils;

public class CRRecoveryRate extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getBaseCRRecoveryRatePercentPerDay();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        // For some reason repair % per day isn't exposed in API ???
        float repairPercent = (float) ReflectionUtils.invokeMethod(spec, "getRepairPercentPerDay");
        return Utils.getSelectionWeightScaledByValue(repairPercent, 5f, true);
    }
}
