package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class DModEffect extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT);
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return 0f;
        return 4f - Utils.hullSizeToInt(spec.getHullSize());
    }
}
