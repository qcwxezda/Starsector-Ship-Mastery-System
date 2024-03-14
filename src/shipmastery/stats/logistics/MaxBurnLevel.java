package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.ReflectionUtils;

public class MaxBurnLevel extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getMaxBurnLevel();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // For some reason max burn also isn't exposed in the API?????
        float maxBurn = (float) ReflectionUtils.invokeMethod(spec, "getMaxBurn");
        float weight = 1f;
        if (maxBurn <= 10f) weight *= 1.25f;
        if (maxBurn <= 9f) weight *= 1.25f;
        if (maxBurn <= 8f) weight *= 1.5f;
        if (maxBurn <= 7f) weight *= 1.5f;
        if (maxBurn <= 6f) weight *= 1.5f;
        return weight;
    }
}
