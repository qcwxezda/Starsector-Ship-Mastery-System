
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class HullPoints extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getHullBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.getHitpoints() <= 750f) return 0f;
        if (spec.getHitpoints() <= 1500f) return 0.5f;
        return 1f;
    }
}
