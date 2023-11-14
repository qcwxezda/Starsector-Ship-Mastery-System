package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.ListMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Utils;

import java.io.IOException;
import java.util.*;

public abstract class ShipMastery {

    public static class MasteryData {
        /** Count of MP currency */
        public float points;
        /** Mastery level attained */
        public int level;
        /** Set of mastery levels whose effects are active */
        public SortedSet<Integer> activeLevels;

        public MasteryData(float points, int level) {
            this.points = points;
            this.level = level;
            activeLevels = new TreeSet<>();
        }
    }

    /** Maps base hull spec ids to structure containing mastery data for that hull spec */
    public static class MasteryTable extends HashMap<String, MasteryData> {}
    public static final String MASTERY_KEY = "shipmastery_Mastery";
    public static final String DEFAULT_PRESET_NAME = "default";
    public static MasteryTable MASTERY_TABLE;

    /** Maps mastery effect classes to their ids */
    public static Map<Class<?>, String> effectToIdMap = new HashMap<>();
    /** Maps mastery effect ids to their classes */
    public static Map<String, Class<?>> idToEffectMap = new HashMap<>();
    /** Maps preset names to list of values. */
    public static ListMap<String> presetsMap = new ListMap<>();
    /** Maps hull ids to list of mastery effects */
    public static ListMap<MasteryEffect> masteryMap = new ListMap<>();

    public static void loadMasteryData()
            throws JSONException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        JSONArray masteryList = Global.getSettings().getMergedSpreadsheetDataForMod("id", "data/shipmastery/mastery_list.csv", "shipmasterysystem");

        Utils.populateHullIdMap();

        for (int i = 0; i < masteryList.length(); i++) {
            JSONObject item = masteryList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("script");
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            effectToIdMap.put(cls, id);
            idToEffectMap.put(id, cls);
        }

        JSONObject masteryPresets = Global.getSettings().getMergedJSONForMod("data/shipmastery/mastery_presets.json", "shipmasterysystem");
        //noinspection unchecked
        Iterator<String> itr = masteryPresets.keys();
        while (itr.hasNext()) {
            String key = itr.next();
            JSONArray value = masteryPresets.getJSONArray(key);
            for (int i = 0; i < value.length(); i++) {
                presetsMap.add(key, value.getString(i));
            }
        }

        for (String hullId : Utils.hullIdToBaseHullIdMap.values()) {
            for (String str : presetsMap.get(DEFAULT_PRESET_NAME)) {
                String[] params = str.trim().split("\\s+");
                Class<?> cls = idToEffectMap.get(params[0]);
                MasteryEffect effect = (MasteryEffect) cls.newInstance();
                System.out.println(Arrays.toString(Arrays.copyOfRange(params, 1, params.length)));
                effect.init(Arrays.copyOfRange(params, 1, params.length));
                masteryMap.add(hullId, effect);
            }
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
    }

    public static Class<?> getEffectClass(String id) {
        return idToEffectMap.get(id);
    }

    public static String getId(Class<?> effectClass) {
        return effectToIdMap.get(effectClass);
    }

}
