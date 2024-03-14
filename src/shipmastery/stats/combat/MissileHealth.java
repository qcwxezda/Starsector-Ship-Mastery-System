
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class MissileHealth extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getMissileHealthBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        // Count number of missile slots
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = wsc.sm + wsc.mm + wsc.lm;
        return Utils.getSelectionWeightScaledByValue(count, 3f, false);
    }
}
