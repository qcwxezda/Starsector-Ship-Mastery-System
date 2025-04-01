package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class SensorStrength extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSensorStrength();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return !spec.isCivilianNonCarrier() ? 0.25f : 0.5f;
    }
}
