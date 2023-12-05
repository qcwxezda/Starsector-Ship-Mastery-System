
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
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        if (!Utils.hasShield(spec)) return 0f;
        // Prefer shields with greater arc
        return (float) Math.log(spec.getShieldSpec().getArc() + 2f);
    }
}
