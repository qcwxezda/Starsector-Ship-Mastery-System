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
    public final NavigableMap<Integer, String> activeLevels;

    public SaveData(float points, int level) {
        this.points = points;
        this.level = level;
        activeLevels = new TreeMap<>();
    }

    public boolean activateLevel(int level, String id) {
        return !id.equals(activeLevels.put(level, id));
    }

    public boolean deactivateLevel(int level) {
        return activeLevels.remove(level) != null;
    }
}
