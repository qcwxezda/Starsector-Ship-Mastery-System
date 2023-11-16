package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import shipmastery.stats.ShipStat;

public class FluxDissipation extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getFluxDissipation();
    }
}
