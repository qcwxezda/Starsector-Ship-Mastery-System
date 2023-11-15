package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.MasteryUtils;
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
        public NavigableSet<Integer> activeLevels;

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
    private static MasteryTable MASTERY_TABLE;

    /** Maps mastery effect classes to their ids */
    private static final Map<Class<?>, String> effectToIdMap = new HashMap<>();
    /** Maps mastery effect ids to their classes */
    private static final Map<String, Class<?>> idToEffectMap = new HashMap<>();
    /** Preset name -> Mastery level -> initialization strings */
    private static final Utils.ListMapMap<String, Integer, String> presetsMap = new Utils.ListMapMap<>();
    /** Hull id -> Mastery level -> Mastery effects */
    private static final Utils.ListMapMap<String, Integer, MasteryEffect> masteryMap = new Utils.ListMapMap<>();
    /** Effect id -> set of tags */
    private static final Map<String, Set<String>> tagMap = new HashMap<>();
    /** Effect id -> tier */
    private static final Map<String, Integer> tierMap = new HashMap<>();

    public static int getMaxMastery(ShipHullSpecAPI spec) {
        String id = Utils.getBaseHullId(spec);
        return masteryMap.get(id).size();
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

        List<MasteryEffect> effects = getMasteryEffects(spec, data.level);
        boolean autoActivate = true;
        for (MasteryEffect effect : effects) {
            if (!MasteryUtils.isAutoActivate(effect)) {
                autoActivate = false;
                break;
            }
        }
        if (autoActivate) {
            activateMastery(spec, data.level);
        }
    }

    public static float getMasteryPoints(ShipHullSpecAPI spec) {
        if (MASTERY_TABLE == null) return 0f;

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

    public static void activateMastery(ShipHullSpecAPI spec, int level) {
        String id = Utils.getBaseHullId(spec);
        MasteryData data = MASTERY_TABLE.get(id);

        if (data == null) {
            data = new MasteryData(0, 0);
            MASTERY_TABLE.put(id, data);
        }

        data.activeLevels.add(level);
        List<MasteryEffect> effects = getMasteryEffects(spec, level);
        for (int i = 0; i < effects.size(); i++) {
            MasteryEffect effect = effects.get(i);
            effect.onActivate(spec, MasteryUtils.makeEffectId(effect, level, i));
        }
    }

    public static void deactivateMastery(ShipHullSpecAPI spec, int level) {
        String id = Utils.getBaseHullId(spec);
        MasteryData data = MASTERY_TABLE.get(id);

        if (data == null) {
            data = new MasteryData(0, 0);
            MASTERY_TABLE.put(id, data);
        }

        data.activeLevels.remove(level);
        List<MasteryEffect> effects = getMasteryEffects(spec, level);
        for (int i = 0; i < effects.size(); i++) {
            MasteryEffect effect = effects.get(i);
            effect.onDeactivate(spec, MasteryUtils.makeEffectId(effect, level, i));
        }
    }

    /** Returns a copy of the original data */
    public static NavigableSet<Integer> getActiveMasteries(ShipHullSpecAPI spec) {
        if (MASTERY_TABLE == null) return new TreeSet<>();

        MasteryData data = MASTERY_TABLE.get(Utils.getBaseHullId(spec));
        return data == null ? new TreeSet<Integer>() : new TreeSet<>(data.activeLevels);
    }

    /** Returns a copy of the original data */
    public static List<MasteryEffect> getMasteryEffects(ShipHullSpecAPI spec, int level) {
        String id = Utils.getBaseHullId(spec);
        return new ArrayList<>(masteryMap.get(id, level - 1));
    }

    public static void loadMasteryData()
            throws JSONException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        JSONArray masteryList = Global.getSettings().getMergedSpreadsheetData("id", "data/shipmastery/mastery_list.csv");

        Utils.populateHullIdMap();

        for (int i = 0; i < masteryList.length(); i++) {
            JSONObject item = masteryList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("script");
            String tags = item.getString("tags");
            int tier = item.optInt("tier", 1);
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            effectToIdMap.put(cls, id);
            idToEffectMap.put(id, cls);
            tagMap.put(id, new HashSet<>(Arrays.asList(tags.split("\\s+"))));
            tierMap.put(id, tier);
        }

        JSONObject masteryPresets = Global.getSettings().getMergedJSON("data/shipmastery/mastery_presets.json");
        //noinspection unchecked
        Iterator<String> itr = masteryPresets.keys();
        while (itr.hasNext()) {
            String key = itr.next();
            JSONArray value = masteryPresets.getJSONArray(key);
            for (int i = 0; i < value.length(); i++) {
                Object o = value.get(i);
                if (o instanceof JSONArray) {
                    JSONArray subValue = (JSONArray) o;
                    for (int j = 0; j < subValue.length(); j++) {
                        presetsMap.add(key, i, subValue.getString(j));
                    }
                }
                else if (o instanceof String) {
                    presetsMap.add(key, i, (String) o);
                }
                // Only string or array of strings allowed
                else {
                    throw new RuntimeException("Expected JSON array or String, received: " + o);
                }
            }
        }

        for (String hullId : Utils.hullIdToBaseHullIdMap.values()) {
            for (Map.Entry<Integer, List<String>> entry : presetsMap.get(DEFAULT_PRESET_NAME).entrySet()) {
                int level = entry.getKey();
                List<String> strings = entry.getValue();
                for (String str : strings) {
                    String[] params = str.trim().split("\\s+");
                    Class<?> cls = idToEffectMap.get(params[0]);
                    MasteryEffect effect = (MasteryEffect) cls.newInstance();
                    effect.init(Arrays.copyOfRange(params, 1, params.length));
                    masteryMap.add(hullId, level, effect);
                }
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

    /** All instances of a mastery effect (with the same class) share the same tags. */
    public static boolean hasTag(MasteryEffect effect, String tag) {
        Set<String> tags = getTags(effect);
        return (tags != null && tags.contains(tag));
    }

    /** Returns a modifiable reference */
    public static Set<String> getTags(MasteryEffect effect) {
        return tagMap.get(getId(effect.getClass()));
    }

}
