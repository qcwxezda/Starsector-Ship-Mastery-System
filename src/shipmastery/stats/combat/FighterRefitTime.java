
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class FighterRefitTime extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getFighterRefitTimeMult();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getFighterBays() <= 0) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(spec.getFighterBays() / (1f + Utils.hullSizeToInt(spec.getHullSize())), 0f, 0.5f, 2f);
    }
}
