package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class Maneuverability extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getAcceleration(), stats.getDeceleration(), stats.getMaxTurnRate(), stats.getTurnAcceleration()};
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        // Prefer less maneuverable ships
        ShipHullSpecAPI.EngineSpecAPI engine = spec.getEngineSpec();
        return 200f / engine.getAcceleration() + 200f / engine.getDeceleration() + 200f / engine.getMaxTurnRate() + 200f / engine.getTurnAcceleration() + 1f;
    }
}
