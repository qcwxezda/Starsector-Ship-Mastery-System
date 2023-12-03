package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class AllWeaponOPCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(Arrays.asList(
                stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_MISSILE_MOD),
                stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD)
                                            ));
    }
}
