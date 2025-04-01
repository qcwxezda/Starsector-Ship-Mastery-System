package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class MissileWeaponsOPCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[]{
                stats.getDynamic().getMod(Stats.SMALL_MISSILE_MOD),
                stats.getDynamic().getMod(Stats.MEDIUM_MISSILE_MOD),
                stats.getDynamic().getMod(Stats.LARGE_MISSILE_MOD)
        };
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (!Utils.getDominantWeaponTypes(spec).contains(WeaponAPI.WeaponType.MISSILE)) return null;
        return 1f;
    }
}
