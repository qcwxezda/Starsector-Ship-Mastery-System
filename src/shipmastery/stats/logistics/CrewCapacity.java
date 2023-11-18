package shipmastery.stats.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class CrewCapacity extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getMaxCrewMod();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        float weight = (float) Math.log(spec.getMaxCrew() + 2f);
        if (spec.isCivilianNonCarrier()) weight *= 3f;
        return weight;
    }
}