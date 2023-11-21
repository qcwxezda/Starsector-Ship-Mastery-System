package shipmastery.data;

import java.util.NavigableMap;
import java.util.TreeMap;

public class SaveData {
    /**
     * Count of MP currency
     */
    public float points;
    /**
     * Mastery level attained
     */
    public int level;
    /**
     * Set of mastery levels whose effects are active
     */
    public NavigableMap<Integer, Boolean> activeLevels;

    public SaveData(float points, int level) {
        this.points = points;
        this.level = level;
        activeLevels = new TreeMap<>();
    }

    public void activateLevel(int level, boolean isOption2) {
        activeLevels.put(level, isOption2);
    }

    public void deactivateLevel(int level) {
        activeLevels.remove(level);
    }
}
