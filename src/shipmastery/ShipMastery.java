package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryEffect;
import shipmastery.stats.ShipStat;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.io.IOException;
import java.util.*;

public abstract class ShipMastery {

    public static class MasteryData {
        /** Count of MP currency */
        float points;
        /** Mastery level attained */
        int level;
        /** Set of mastery levels whose effects are active */
        NavigableSet<Integer> activeLevels;

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
    /** Ship stat id -> singleton object */
    private static final Map<String, ShipStat> statSingletonMap = new HashMap<>();


    public static int getMaxMastery(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        SortedMap<Integer, List<MasteryEffect>> count = masteryMap.get(id);
        return count == null ? 0 : count.size();
    }

    public static int getMasteryLevel(ShipHullSpecAPI spec) {
        if (MASTERY_TABLE == null) return 0;

        MasteryData data = MASTERY_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? 0 : data.level;
    }

    public static void advanceMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
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

        MasteryData data = MASTERY_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? 0 : data.points;
    }

    public static void addMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getRestoredHullSpecId(spec);
        MasteryData data = MASTERY_TABLE.get(id);
        if (data == null) {
            MASTERY_TABLE.put(id, new MasteryData(amount, 0));
        }
        else {
            data.points += amount;
        }
    }

    public static void spendMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getRestoredHullSpecId(spec);
        MasteryData data = MASTERY_TABLE.get(id);
        if (data == null) return;

        data.points -= amount;
        data.points = Math.max(0f, data.points);
    }

    public static void activateMastery(ShipHullSpecAPI spec, int level) {
        String id = Utils.getRestoredHullSpecId(spec);
        MasteryData data = MASTERY_TABLE.get(id);

        if (data == null) {
            data = new MasteryData(0, 0);
            MASTERY_TABLE.put(id, data);
        }

        data.activeLevels.add(level);
        List<MasteryEffect> effects = getMasteryEffects(spec, level);
        for (int i = 0; i < effects.size(); i++) {
            MasteryEffect effect = effects.get(i);
            effect.onActivate(MasteryUtils.makeEffectId(effect, level, i));
        }
    }

    public static void deactivateMastery(ShipHullSpecAPI spec, int level) {
        String id = Utils.getRestoredHullSpecId(spec);
        MasteryData data = MASTERY_TABLE.get(id);

        if (data == null) {
            data = new MasteryData(0, 0);
            MASTERY_TABLE.put(id, data);
        }

        data.activeLevels.remove(level);
        List<MasteryEffect> effects = getMasteryEffects(spec, level);
        for (int i = 0; i < effects.size(); i++) {
            MasteryEffect effect = effects.get(i);
            effect.onDeactivate(MasteryUtils.makeEffectId(effect, level, i));
        }
    }

    /** Returns a copy of the original data */
    public static NavigableSet<Integer> getActiveMasteriesCopy(ShipHullSpecAPI spec) {
        if (MASTERY_TABLE == null || spec == null) return new TreeSet<>();

        MasteryData data = MASTERY_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? new TreeSet<Integer>() : new TreeSet<>(data.activeLevels);
    }

    /** This function 1-indexed */
    public static List<MasteryEffect> getMasteryEffects(ShipHullSpecAPI spec, int level) {
        String id = Utils.getRestoredHullSpecId(spec);
        return masteryMap.get(id, level - 1);
    }

    static Map<String, String> tagMap = new HashMap<>();
    static Map<String, Integer> tierMap = new HashMap<>();
    static Map<String, Integer> priorityMap = new HashMap<>();
    static Map<String, Float> defaultStrengthMap = new HashMap<>();

    public static void loadMasteryData()
            throws JSONException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        JSONArray masteryList = Global.getSettings().getMergedSpreadsheetData("id", "data/shipmastery/mastery_list.csv");

        Utils.populateVariantIdToBaseHullIds();

        for (int i = 0; i < masteryList.length(); i++) {
            JSONObject item = masteryList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("script");
            String tags = item.getString("tags");
            int tier = item.optInt("tier", 1);
            int priority = item.optInt("priority", 0);
            float defaultStrength = (float) item.optDouble("default_strength", 0f);
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            effectToIdMap.put(cls, id);
            idToEffectMap.put(id, cls);
            tagMap.put(id, tags);
            tierMap.put(id, tier);
            priorityMap.put(id, priority);
            defaultStrengthMap.put(id, defaultStrength);
        }

        JSONArray statsList = Global.getSettings().getMergedSpreadsheetData("id", "data/shipmastery/stats_list.csv");
        for (int i = 0; i < statsList.length(); i++) {
            JSONObject item = statsList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("location");
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            ShipStat stat = (ShipStat) cls.newInstance();
            stat.name = item.getString("name");
            stat.tier = item.optInt("tier", 1);
            stat.defaultAmount = (float) item.optDouble("default_amount", 1f);
            stat.tags.addAll(Arrays.asList(item.getString("tags").trim().split("\\s+")));
            statSingletonMap.put(id, stat);
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


        // Call onActivate on activated masteries at the beginning of game...


//        for (String hullId : Utils.hullIdToBaseHullIdMap.values()) {
//            int i = 0;
//            for (String id : statSingletonMap.keySet()) {
//                MasteryEffect effect = new ModifyStatsMult();
//                effect.init("" + 1, id, "" + -0.1);
//                masteryMap.add(hullId, i++, effect);
//            }
//        }
    }

    public static void createMasteryEffects() throws InstantiationException, IllegalAccessException {
        masteryMap.clear();

        for (String hullId : new HashSet<>(Utils.variantIdToBaseHullIdMap.values())) {
            for (Map.Entry<Integer, List<String>> entry : presetsMap.get(DEFAULT_PRESET_NAME).entrySet()) {
                int level = entry.getKey();
                List<String> strings = entry.getValue();
                for (String str : strings) {
                    String[] params = str.trim().split("\\s+");
                    String id = params[0];
                    // If no params, use default strength
                    if (params.length == 1) {
                        params = new String[] {id, "" + defaultStrengthMap.get(id)};
                    }
                    Class<?> cls = idToEffectMap.get(id);
                    if (cls == null) {
                        throw new RuntimeException("Unknown effect: " + id);
                    }
                    BaseMasteryEffect effect = (BaseMasteryEffect) cls.newInstance();
                    effect.setTier(tierMap.get(id));
                    effect.setPriority(priorityMap.get(id));
                    //effect.setWeight(weightMap.get(id));
                    effect.setHullSpec(Global.getSettings().getHullSpec(hullId));
                    effect.addTags(tagMap.get(id).trim().split("\\s+"));
                    effect.init(Arrays.copyOfRange(params, 1, params.length));
                    masteryMap.add(hullId, level, effect);
                }
            }
        }
    }

    public static void activateInitialMasteries() {
        for (String hullId : new HashSet<>(Utils.variantIdToBaseHullIdMap.values())) {
            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSettings().getHullSpec(hullId), new MasteryUtils.MasteryAction() {
                        @Override
                        public void perform(MasteryEffect effect, String id) {
                            effect.onActivate(id);
                        }
                    }
            );
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
    public static String getId(Class<?> effectClass) {
        return effectToIdMap.get(effectClass);
    }

    public static ShipStat getStatParams(String id) { return statSingletonMap.get(id); }

    public static void clearInvalidActiveLevels() {
        for (String id : new HashSet<>(Utils.variantIdToBaseHullIdMap.values())) {
            ShipHullSpecAPI spec = Global.getSettings().getHullSpec(id);
            MasteryData data = MASTERY_TABLE.get(id);
            if (data == null) continue;
            Iterator<Integer> itr = data.activeLevels.iterator();
            while (itr.hasNext()) {
                if (itr.next() > ShipMastery.getMaxMastery(spec)) {
                    itr.remove();
                }
            }
        }
    }
}
