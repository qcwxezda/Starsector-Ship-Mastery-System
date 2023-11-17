package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.stats.ShipStat;

public class SupplyUsage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSuppliesPerMonth();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        float weight = spec.getSuppliesPerMonth() + 1f;
        if (spec.getBuiltInMods().contains(HullMods.INCREASED_MAINTENANCE)) weight *= 2f;
        return weight;
    }
}
