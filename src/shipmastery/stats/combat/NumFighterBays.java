package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class NumFighterBays extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getNumFighterBays();
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return 0f;
        return spec.getFighterBays();
    }
}
