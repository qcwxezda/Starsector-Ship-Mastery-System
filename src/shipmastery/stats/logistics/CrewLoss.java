package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class CrewLoss extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getCrewLossMult();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        float weight =  Utils.getSelectionWeightScaledByValueIncreasing(spec.getMaxCrew(),
                0f, 300f, 1500f);
        return spec.isCivilianNonCarrier() ? weight : weight * 0.5f;
    }
}
