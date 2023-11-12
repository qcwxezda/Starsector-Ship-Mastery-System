package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class Settings {

    public static class MasteryData {
        float points;
        int level;

        public MasteryData(float points, int level) {
            this.points = points;
            this.level = level;
        }
    }

    public static class MasteryTable extends HashMap<String, MasteryData> {}
    public static final String MASTERY_KEY = "shipmastery_Mastery";
    public static MasteryTable MASTERY_TABLE;
    public static Color masteryColor = new Color(96, 192, 255);
    public static float doubleClickInterval = 0.75f;

    public static void loadMasteryData() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (!persistentData.containsKey(MASTERY_KEY)) {
            MASTERY_TABLE = new MasteryTable();
            persistentData.put(MASTERY_KEY, MASTERY_TABLE);
        }
        else {
            MASTERY_TABLE = (MasteryTable) persistentData.get(MASTERY_KEY);
        }
    }

    public static float getMasteryPoints(ShipHullSpecAPI spec) {
        MasteryData data = MASTERY_TABLE.get(Utils.getBaseHullId(spec));
        return data == null ? 0 : data.points;
    }

    public static void addMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getBaseHullId(spec);
        MasteryData data = MASTERY_TABLE.get(id);
        if (data == null) {
            MASTERY_TABLE.put(id, new MasteryData(amount, 0));
        }
        else {
            data.points += amount;
        }
    }

    public static void spendMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getBaseHullId(spec);
        MasteryData data = MASTERY_TABLE.get(id);
        if (data == null) return;

        data.points -= amount;
        data.points = Math.max(0f, data.points);
    }
}
