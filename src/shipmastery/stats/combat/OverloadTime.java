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
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return !Utils.hasShield(spec) ? 0f : 1f;
    }
}
