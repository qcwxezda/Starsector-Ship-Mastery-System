package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class CRPeakDuration extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getPeakCRDuration();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return 1.6f * Utils.getSelectionWeightScaledByValue(spec.getNoCRLossSeconds(), 360f, true);
    }
}
