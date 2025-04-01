package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class DModEffect extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getDynamic().getStat(Stats.DMOD_EFFECT_MULT);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return spec.isCivilianNonCarrier() ? 0.25f : 1f;
    }
}
