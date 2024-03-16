package shipmastery.data;

import java.util.ArrayList;
import java.util.List;

/** Mastery data. One instance per ship hull type. */
public class HullMasteryData {
    final List<MasteryLevelData> levels = new ArrayList<>();
    final String hullOrPresetName;
    final int maxLevel;

    public HullMasteryData(String name, int maxLevel) {
        hullOrPresetName = name;
        this.maxLevel = maxLevel;
        for (int i = 1; i <= maxLevel; i++) {
            levels.add(null);
        }
    }

    public MasteryLevelData getDataForLevel(int level) {
        return levels.get(level - 1);
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setLevelData(int level, MasteryLevelData levelData) {
        levels.set(level - 1, levelData);
    }
}
