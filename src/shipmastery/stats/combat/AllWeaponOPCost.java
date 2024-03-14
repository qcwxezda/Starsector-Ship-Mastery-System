package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class AllWeaponOPCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {
                stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD),
                stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD),
                stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_MISSILE_MOD),
                stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD)};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return 1f;
    }
}
