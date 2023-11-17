package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class SensorStrength extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSensorStrength();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        return Utils.hullSizeToInt(spec.getHullSize()) + 1f;
    }
}
