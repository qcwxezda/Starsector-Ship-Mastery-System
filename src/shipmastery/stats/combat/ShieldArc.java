package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShieldArc extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getShieldArcBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.hasShield(spec) || spec.getShieldSpec() == null) return null;
        if (spec.getShieldSpec().getArc() >= 360f) return null;
        // Prefer ships with smaller arcs
        return Utils.getSelectionWeightScaledByValue(spec.getShieldSpec().getArc(), 180f, true);
    }
}
