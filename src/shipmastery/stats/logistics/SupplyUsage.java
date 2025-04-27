package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class SupplyUsage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSuppliesPerMonth();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        float weight = Utils.getSelectionWeightScaledByValueIncreasing(
                spec.getSuppliesPerMonth(),
                0.5f,
                15f,
                50f);
        if (!spec.isCivilianNonCarrier()) weight /= 2f;
        if (spec.getBuiltInMods().contains("high_maintenance")) weight *= 1.5f;
        return weight;
    }
}
