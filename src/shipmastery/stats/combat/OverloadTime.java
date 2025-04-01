package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class OverloadTime extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getOverloadTimeMod();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return !Utils.hasShield(spec) ? null : 1.5f;
    }
}
