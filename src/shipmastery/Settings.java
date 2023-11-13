package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.impl.logistics.AdditionalSMods;
import shipmastery.util.Utils;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Settings {

    public static class MasteryData {
        float points;
        int level;

        public MasteryData(float points, int level) {
            this.points = points;
            this.level = level;
        }
    }

    /** Maps base hull spec ids to structure containing mastery data for that hull spec */
    public static class MasteryTable extends HashMap<String, MasteryData> {}
    public static final String MASTERY_KEY = "shipmastery_Mastery";
    public static MasteryTable MASTERY_TABLE;
    public static Color masteryColor = new Color(96, 192, 255);
    public static float doubleClickInterval = 0.75f;

    /** Maps mastery effect classes to their ids */
    public static Map<Class<?>, String> effectToIdMap = new HashMap<>();
    /** Maps mastery effect ids to their classes */
    public static Map<String, Class<?>> idToEffectMap = new HashMap<>();
    /** Global unlock list -- once a mastery is unlocked once (on any ship), it's known and shown on all other hull types as well */
    public static Set<String> unlockedEffectIds = new HashSet<>();

    public static void loadMasteryData() throws JSONException, IOException, ClassNotFoundException {
        JSONArray masteryList = Global.getSettings().getMergedSpreadsheetDataForMod("id", "data/shipmastery/mastery_list.csv", "shipmasterysystem");

        for (int i = 0; i < masteryList.length(); i++) {
            JSONObject item = masteryList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("script");
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            effectToIdMap.put(cls, id);
            idToEffectMap.put(id, cls);
        }
    }

    public static void loadMasteryTable() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (!persistentData.containsKey(MASTERY_KEY)) {
            MASTERY_TABLE = new MasteryTable();
            persistentData.put(MASTERY_KEY, MASTERY_TABLE);
        }
        else {
            MASTERY_TABLE = (MasteryTable) persistentData.get(MASTERY_KEY);
        }

        // Populate unlocked effect ids
        unlockedEffectIds.clear();
        for (Map.Entry<String, MasteryData> entry : MASTERY_TABLE.entrySet()) {
            for (int i = 1; i <= entry.getValue().level; i++) {
                unlockedEffectIds.add(getId(
                        getMasteryEffect(Global.getSettings().getHullSpec(entry.getKey()), i).getClass()));
            }
        }
    }

    public static int getMasteryLevel(ShipHullSpecAPI spec) {
        if (MASTERY_TABLE == null) return 0;

        MasteryData data = MASTERY_TABLE.get(Utils.getBaseHullId(spec));
        return data == null ? 0 : data.level;
    }

    public static void advanceMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getBaseHullId(spec);
        MasteryData data = MASTERY_TABLE.get(id);

        if (data == null) {
            data = new MasteryData(0, 1);
            MASTERY_TABLE.put(id, data);
        } else {
            data.level++;
        }

        String effectId = getId(getMasteryEffect(spec, data.level).getClass());
        unlockedEffectIds.add(effectId);
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

    public static Class<?> getEffectClass(String id) {
        return idToEffectMap.get(id);
    }

    public static String getId(Class<?> effectClass) {
        return effectToIdMap.get(effectClass);
    }

    public static MasteryEffect getMasteryEffect(ShipHullSpecAPI spec, int level) {
        // Table lookup...
        return new AdditionalSMods().setStrength(1f);
    }
}
