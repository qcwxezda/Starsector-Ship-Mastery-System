
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class WeaponTurnRate extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getWeaponTurnRateBonus();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return 1f;
    }
}
