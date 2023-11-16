package shipmastery.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import java.util.HashSet;
import java.util.Set;

public abstract class ShipStat {
    public String name = "NONE";
    public int tierOverride = 1;
    public float weight = 1f;
    public float defaultAmount = 1f;
    public Set<String> tags = new HashSet<>();
    /** Type: StatBonus | MutableStat | List<StatBonus | MutableStat>. */
    public abstract Object get(MutableShipStatsAPI stats);

    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ShipStat)) return false;
        return name.equals(((ShipStat) other).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
