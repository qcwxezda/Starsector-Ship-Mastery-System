
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class EMPDamageTaken extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getEmpDamageTakenMult(), stats.getWeaponDamageTakenMult(), stats.getEngineDamageTakenMult()};
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        // Prefer ships with more armor
        return (float) Math.log(spec.getArmorRating() + 2f);
    }
}
