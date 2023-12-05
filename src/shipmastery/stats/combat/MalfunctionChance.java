package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class MalfunctionChance extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getWeaponMalfunctionChance(), stats.getEngineMalfunctionChance(), stats.getShieldMalfunctionChance()};
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        // Prefer ships with low CR
        return Math.min(10f, 2000f / (spec.getNoCRLossSeconds() + 1f));
    }
}
