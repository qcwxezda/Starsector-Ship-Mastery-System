
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class RepairTime extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[]{stats.getCombatEngineRepairTimeMult(), stats.getCombatWeaponRepairTimeMult()};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return 1.2f;
    }
}
