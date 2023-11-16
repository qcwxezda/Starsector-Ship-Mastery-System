
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class PhaseSpeedAcceleration extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(
                Arrays.asList(stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD), stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD)));
    }

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return spec.getShieldType() == ShieldAPI.ShieldType.PHASE;
    }
}
