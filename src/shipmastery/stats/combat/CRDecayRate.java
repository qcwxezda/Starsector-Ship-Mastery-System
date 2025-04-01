package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class CRDecayRate extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getCRLossPerSecondPercent();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValueDecreasing(spec.getNoCRLossSeconds(), 60f, 240f, 1200f);
    }
}
