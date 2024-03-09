package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class BallisticWeaponsOPCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[]{
                stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD)
        };
    }
}
