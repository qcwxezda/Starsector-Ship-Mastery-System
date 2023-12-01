package shipmastery.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import java.util.HashSet;
import java.util.Set;

public abstract class ShipStat {
    public String name = "NONE";
    public int tier = 1;
    public float defaultAmount = 1f;
    public Set<String> tags = new HashSet<>();
    /** Type: StatBonus | MutableStat | List<StatBonus | MutableStat>. */
    public abstract Object get(MutableShipStatsAPI stats);

    /** True if it modifies OP costs, hangar space, anything that requires the variant to be refitted */
    public boolean triggersAutofit() {
        return false;
    }

    /** Selection weight for each stat will be normalized across all hull specs. */
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        return 1f;
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
