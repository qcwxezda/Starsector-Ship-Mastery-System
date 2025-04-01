
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
        float weight = wsc.sm + 2f * wsc.mm + 4f * wsc.lm;
        if (weight <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(weight, 0f, 3f, 12f);
    }
}
