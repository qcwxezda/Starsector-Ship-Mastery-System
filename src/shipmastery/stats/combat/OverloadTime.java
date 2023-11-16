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
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return Utils.hasShield(spec);
    }
}
