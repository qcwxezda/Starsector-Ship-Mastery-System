package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class EnergyWeaponsOPCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[]{
                stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD)
        };
    }
}
