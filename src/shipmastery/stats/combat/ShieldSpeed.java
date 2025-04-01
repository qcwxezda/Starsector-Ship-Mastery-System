
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShieldSpeed extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getShieldTurnRateMult(), stats.getShieldUnfoldRateMult()};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.hasShield(spec)) return null;
        // Prefer ships with higher shield arc
        return Utils.getSelectionWeightScaledByValueIncreasing(spec.getShieldSpec().getArc(), 0f, 90f, 720f);
    }
}
