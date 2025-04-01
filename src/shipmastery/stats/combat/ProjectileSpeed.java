package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ProjectileSpeed extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getProjectileSpeedMult();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        Utils.WeaponSlotCount wsc = Utils.countWeaponSlots(spec);
        float weightBallistic = wsc.computeWeaponWeight(WeaponAPI.WeaponType.BALLISTIC, 0.2f, 0.3f);
        float weightEnergy = wsc.computeWeaponWeight(WeaponAPI.WeaponType.ENERGY, 0.2f, 0.3f);
        if (weightBallistic + weightEnergy <= 0f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(0.5f*weightBallistic + 0.5f*weightEnergy, 0f, 0.6f, 1f);
    }
}
