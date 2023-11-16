package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import shipmastery.stats.ShipStat;

public class SupplyUsage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSuppliesPerMonth();
    }
}
