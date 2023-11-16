
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class PhaseCloakCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(
                Arrays.asList(stats.getPhaseCloakActivationCostBonus(), stats.getPhaseCloakUpkeepCostBonus()));
    }

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return spec.getShieldType() == ShieldAPI.ShieldType.PHASE;
    }
}
