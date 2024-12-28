package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class MaxSpeed extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getMaxSpeed();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getEngineSpec().getMaxSpeed(), 70f, true);
    }
}
