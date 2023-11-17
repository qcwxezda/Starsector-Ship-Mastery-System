
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class MissileSpeed extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(
                Arrays.asList(stats.getMissileMaxTurnRateBonus(), stats.getMissileTurnAccelerationBonus(),
                              stats.getMissileMaxSpeedBonus(), stats.getMissileAccelerationBonus(),
                              stats.getMissileWeaponRangeBonus()));
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        // Count number of missile slots
        int cnt = 0;
        for (WeaponSlotAPI slot : spec.getAllWeaponSlotsCopy()) {
            if (slot.getWeaponType() == WeaponAPI.WeaponType.MISSILE || slot.getWeaponType() == WeaponAPI.WeaponType.COMPOSITE || slot.getWeaponType() ==
                    WeaponAPI.WeaponType.SYNERGY || slot.getWeaponType() == WeaponAPI.WeaponType.UNIVERSAL) {
                cnt++;
            }
        }
        return cnt;
    }
}
