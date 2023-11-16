
package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;

public class ShieldSpeed extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(Arrays.asList(stats.getShieldTurnRateMult(), stats.getShieldUnfoldRateMult()));
    }

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return Utils.hasShield(spec);
    }
}
