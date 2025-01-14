package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class DModAcquireProbability extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getDynamic().getMod(Stats.DMOD_ACQUIRE_PROB_MOD);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        float weight = 3.5f - Utils.hullSizeToInt(spec.getHullSize());
        if (spec.isCivilianNonCarrier()) weight *= 0.25f;
        return weight;
    }
}
