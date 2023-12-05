package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class HullArmorDamageTaken extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getHullDamageTakenMult(), stats.getArmorDamageTakenMult()};
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return (float) Math.log(spec.getArmorRating() + spec.getHitpoints() / 10f + 2f);
    }
}
