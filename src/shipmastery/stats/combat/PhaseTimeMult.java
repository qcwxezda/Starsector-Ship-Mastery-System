
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class PhaseTimeMult extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getDynamic().getStat(Stats.PHASE_TIME_BONUS_MULT);
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return 0f;
        return spec.isPhase() ? 1f : 0f;
    }
}
