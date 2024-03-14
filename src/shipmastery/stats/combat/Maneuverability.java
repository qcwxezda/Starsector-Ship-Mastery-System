package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class Maneuverability extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getAcceleration(), stats.getDeceleration(), stats.getMaxTurnRate(), stats.getTurnAcceleration()};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        // Prefer less maneuverable ships
        ShipHullSpecAPI.EngineSpecAPI engine = spec.getEngineSpec();
        return Utils.getSelectionWeightScaledByValue(engine.getMaxTurnRate(), 20f, true);
    }
}
