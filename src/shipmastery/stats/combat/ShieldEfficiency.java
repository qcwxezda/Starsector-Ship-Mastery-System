
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShieldEfficiency extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getShieldDamageTakenMult();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        if (!Utils.hasShield(spec)) return 0f;
        // Prefer shields with already good efficiency
        return Math.max(0.5f, 1.5f - spec.getBaseShieldFluxPerDamageAbsorbed());
    }
}
