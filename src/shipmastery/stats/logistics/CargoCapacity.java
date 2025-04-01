package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class CargoCapacity extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getCargoMod();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (!spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(
                spec.getCargo(), 0f, 500f, 1500f);
    }
}
