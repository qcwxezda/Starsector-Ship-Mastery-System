
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class RepairTime extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[]{stats.getCombatEngineRepairTimeMult(), stats.getCombatWeaponRepairTimeMult()};
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return 1f;
    }
}
