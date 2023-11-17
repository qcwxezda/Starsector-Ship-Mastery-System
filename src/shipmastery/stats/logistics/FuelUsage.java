package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class FuelUsage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getFuelUseMod();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        return spec.getFuelPerLY();
    }
}
