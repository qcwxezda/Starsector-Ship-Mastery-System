package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class MaxBurnLevel extends ShipStat {

    private static MethodHandle getMaxBurn;

    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getMaxBurnLevel();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // For some reason max burn also isn't exposed in the API?????
        float maxBurn = 100f;
        if (getMaxBurn == null) {
            try {
                getMaxBurn = MethodHandles.lookup().findVirtual(spec.getClass(), "getMaxBurn", MethodType.methodType(float.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        if (getMaxBurn != null) {
            try {
                maxBurn = (float) getMaxBurn.invoke(spec);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        float weight = 0.2f;
        if (maxBurn <= 10f) weight *= 1.5f;
        if (maxBurn <= 9f) weight *= 1.5f;
        if (maxBurn <= 8f) weight *= 1.5f;
        if (maxBurn <= 7f) weight *= 1.5f;
        if (maxBurn <= 6f) weight *= 1.5f;
        return weight;
    }
}
