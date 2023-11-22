package shipmastery.data;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

import java.util.ArrayList;
import java.util.List;

/** Mastery data. One instance per ship hull type. */
public class HullMasteryData {
    final List<MasteryLevelData> levels = new ArrayList<>();
    final ShipHullSpecAPI spec;

    public HullMasteryData(ShipHullSpecAPI spec) {
        this.spec = spec;
    }

    public MasteryLevelData getDataForLevel(int level) {
        return levels.get(level);
    }

    public int getMaxLevel() {
        return levels.size();
    }

    public void addLevelData(MasteryLevelData levelData) {
        levels.add(levelData);
    }
}
