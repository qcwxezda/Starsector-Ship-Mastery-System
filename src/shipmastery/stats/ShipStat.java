package shipmastery.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import java.util.HashSet;
import java.util.Set;

public abstract class ShipStat {
    public String name = "NONE";
    public int tier = 1;
    public float defaultAmount = 1f;
    public final Set<String> tags = new HashSet<>();
    /** StatBonus, MutableStat, or an array of StatBonuses or MutableStats */
    public abstract Object get(MutableShipStatsAPI stats);

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
