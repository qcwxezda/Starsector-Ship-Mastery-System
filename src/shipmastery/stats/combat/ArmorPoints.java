
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ArmorPoints extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getArmorBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return Utils.getSelectionWeightScaledByValueDecreasing(Utils.getShieldToHullArmorRatio(spec), 0f, 1f, 2.5f);
    }
}
