
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class PhaseCloakCooldown extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getPhaseCloakCooldownBonus();
    }

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return spec.getShieldType() == ShieldAPI.ShieldType.PHASE;
    }
}
