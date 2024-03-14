
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;

public class PhaseCloakCooldown extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getPhaseCloakCooldownBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        return spec.isPhase() ? 2f : null;
    }
}
