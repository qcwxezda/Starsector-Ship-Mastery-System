
package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class CRMax extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getMaxCombatReadiness();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        return 1.6f;
    }
}
