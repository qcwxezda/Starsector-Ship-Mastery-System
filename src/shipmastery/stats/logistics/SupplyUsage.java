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
        float weight = Utils.getSelectionWeightScaledByValue(spec.getSuppliesPerMonth(), 8f, false);
        if (spec.getBuiltInMods().contains("high_maintenance")) weight *= 2f;
        return weight;
    }
}
