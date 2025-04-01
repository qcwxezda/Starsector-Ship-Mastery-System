
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShieldDamage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getShieldDamageTakenMult();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.hasShield(spec)) return null;
        // Prefer ships with already-good shields
        return Utils.getSelectionWeightScaledByValueDecreasing(spec.getBaseShieldFluxPerDamageAbsorbed(), 0.4f, 0.7f, 1.2f);
    }
}
