
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class FighterRefitTime extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getFighterRefitTimeMult();
    }

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return spec.getFighterBays() > 0;
    }
}
