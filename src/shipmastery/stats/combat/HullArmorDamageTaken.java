package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class HullArmorDamageTaken extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(Arrays.asList(stats.getHullDamageTakenMult(), stats.getArmorDamageTakenMult()));
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return (float) Math.log(spec.getArmorRating() + spec.getHitpoints() / 10f + 2f);
    }
}
