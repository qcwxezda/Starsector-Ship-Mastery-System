package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class FuelUsage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getFuelUseMod();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        float weight = Utils.getSelectionWeightScaledByValueIncreasing(spec.getFuelPerLY(), 1f,  4f, 20f);
        return spec.isCivilianNonCarrier() ? weight : weight * 0.5f;
    }
}
