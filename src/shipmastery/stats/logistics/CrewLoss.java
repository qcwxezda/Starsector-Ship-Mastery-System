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
        if (spec.getMaxCrew() <= 0) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getMaxCrew(), 300f, false);
    }
}
