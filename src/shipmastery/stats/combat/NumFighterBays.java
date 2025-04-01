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
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        int modularBays = spec.getFighterBays() - spec.getBuiltInWings().size();
        if (modularBays <= 0) return null;
        return 1f;
    }
}
