package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class MalfunctionChance extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getWeaponMalfunctionChance(), stats.getEngineMalfunctionChance(), stats.getShieldMalfunctionChance()};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        // Prefer ships with low CR
        return Utils.getSelectionWeightScaledByValueDecreasing(spec.getNoCRLossSeconds(), 30f, 360f, 1080f);
    }
}
