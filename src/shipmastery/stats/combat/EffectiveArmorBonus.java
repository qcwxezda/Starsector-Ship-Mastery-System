package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class EffectiveArmorBonus extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getEffectiveArmorBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // Don't select this normally, it's really just for dwellers
        return 0f;
    }
}
