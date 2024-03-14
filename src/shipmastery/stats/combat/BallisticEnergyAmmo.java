
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class BallisticEnergyAmmo extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {stats.getBallisticAmmoBonus(), stats.getEnergyAmmoBonus()};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float count = wsc.se + wsc.me + wsc.le + wsc.sb + wsc.mb + wsc.lb;
        if (count <= 0f) return null;
        return Utils.getSelectionWeightScaledByValue(count, 6f, false);
    }
}
