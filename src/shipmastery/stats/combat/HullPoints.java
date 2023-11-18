
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
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return (float) Math.log(spec.getHitpoints() + 2f);
    }
}