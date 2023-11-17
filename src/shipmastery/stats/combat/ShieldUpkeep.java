
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShieldUpkeep extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getShieldUpkeepMult();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        if (!Utils.hasShield(spec)) return 0f;
        // Prefer ships with higher shield upkeep
        return (float) Math.log(spec.getShieldSpec().getUpkeepCost() + 2f);
    }
}
