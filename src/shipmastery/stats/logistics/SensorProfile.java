package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class SensorProfile extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSensorProfile();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return Utils.hullSizeToInt(spec.getHullSize()) + 1f;
    }
}
