package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class CRRecoveryRate extends ShipStat {

    private static MethodHandle getRepairPercentPerDay;

    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getBaseCRRecoveryRatePercentPerDay();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        // For some reason repair % per day isn't exposed in API ???
        float repairPercent = 0f;
        if (getRepairPercentPerDay == null) {
            try {
                getRepairPercentPerDay = MethodHandles.lookup().findVirtual(spec.getClass(), "getRepairPercentPerDay",
                                                                            MethodType.methodType(float.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (getRepairPercentPerDay != null) {
            try {
                repairPercent = (float) getRepairPercentPerDay.invoke(spec);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return Utils.getSelectionWeightScaledByValueDecreasing(repairPercent,
                2f, 6f, 25f);
    }
}
