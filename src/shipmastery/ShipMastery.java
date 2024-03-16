package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.data.*;
import shipmastery.mastery.MasteryEffect;
import shipmastery.plugin.ModPlugin;
import shipmastery.stats.ShipStat;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("unchecked")
public abstract class ShipMastery {

    /**
     * Maps base hull spec ids to structure containing mastery data for that hull spec
     */
    public static class SaveDataTable extends HashMap<String, SaveData> {}

    public static final String MASTERY_KEY = "shipmastery_Mastery";
    public static final String DEFAULT_PRESET_NAME = "_DEFAULT_";
    private static SaveDataTable SAVE_DATA_TABLE;


    /**
     * Ship stat id -> singleton object
     */
    private static final Map<String, ShipStat> statSingletonMap = new HashMap<>();
    private static final Map<Class<?>, String> effectToIdMap = new HashMap<>();

    private static final Map<String, HullMasteryData> masteryMap = new HashMap<>();
    private static JSONObject masteryAssignments;
    private static final Map<String, MasteryInfo> masteryInfoMap = new HashMap<>();

    public static int getMaxMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData data = masteryMap.get(id);
        return data == null ? 0 : data.getMaxLevel();
    }

    public static int getPlayerMasteryLevel(ShipHullSpecAPI spec) {
        if (SAVE_DATA_TABLE == null) return 0;

        SaveData data = SAVE_DATA_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? 0 : data.level;
    }

    public static void advancePlayerMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);

        if (data == null) {
            data = new SaveData(0, 1);
            SAVE_DATA_TABLE.put(id, data);
        } else {
            data.level++;
        }

        List<MasteryEffect> effects1 = getMasteryEffects(spec, data.level, false);
        List<MasteryEffect> effects2 = getMasteryEffects(spec, data.level, true);
        if (effects2.isEmpty()) {
            boolean autoActivate = true;
            for (MasteryEffect effect : effects1) {
                if (!MasteryUtils.isAutoActivate(effect)) {
                    autoActivate = false;
                    break;
                }
            }
            if (autoActivate) {
                activatePlayerMastery(spec, data.level, false);
            }
        }
    }

    public static float getPlayerMasteryPoints(ShipHullSpecAPI spec) {
        if (SAVE_DATA_TABLE == null) return 0f;

        SaveData data = SAVE_DATA_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? 0 : data.points;
    }

    public static void addPlayerMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);
        if (data == null) {
            SAVE_DATA_TABLE.put(id, new SaveData(amount, 0));
        } else {
            data.points += amount;
        }
    }

    public static void spendPlayerMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);
        if (data == null) return;

        data.points -= amount;
        data.points = Math.max(0f, data.points);
    }

    public static void activatePlayerMastery(ShipHullSpecAPI spec, int level, boolean isOption2) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);

        if (data == null) {
            data = new SaveData(0, 0);
            SAVE_DATA_TABLE.put(id, data);
        }

        data.activateLevel(level, isOption2);
        List<MasteryEffect> effects = getMasteryEffects(spec, level, isOption2);
        for (MasteryEffect effect : effects) {
            effect.onActivate(Global.getSector().getPlayerPerson());
        }
    }

    public static void deactivatePlayerMastery(ShipHullSpecAPI spec, int level, boolean isOption2) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);

        if (data == null) {
            data = new SaveData(0, 0);
            SAVE_DATA_TABLE.put(id, data);
        }

        data.deactivateLevel(level);
        List<MasteryEffect> effects = getMasteryEffects(spec, level, isOption2);
        for (MasteryEffect effect : effects) {
            effect.onDeactivate(Global.getSector().getPlayerPerson());
        }
    }

    /**
     * Returns a copy of the original data
     */
    public static NavigableMap<Integer, Boolean> getPlayerActiveMasteriesCopy(ShipHullSpecAPI spec) {
        if (SAVE_DATA_TABLE == null || spec == null) return new TreeMap<>();

        SaveData data = SAVE_DATA_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? new TreeMap<Integer, Boolean>() : new TreeMap<>(data.activeLevels);
    }

    /**
     * This function is 1-indexed
     */
    public static List<MasteryEffect> getMasteryEffects(ShipHullSpecAPI spec, int level, boolean isOption2) {
        MasteryLevelData levelData = getLevelData(spec, level);
        if (levelData == null) return new ArrayList<>();
        return isOption2 ? levelData.getEffectsListOption2() : levelData.getEffectsListOption1();
    }

    public static List<MasteryGenerator> getGenerators(ShipHullSpecAPI spec, int level, boolean isOption2) {
        MasteryLevelData levelData = getLevelData(spec, level);
        if (levelData == null) return new ArrayList<>();
        return isOption2 ? levelData.getGeneratorsOption2() : levelData.getGeneratorsOption1();
    }

    private static MasteryLevelData getLevelData(ShipHullSpecAPI spec, int level) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData masteryData = masteryMap.get(id);
        if (masteryData == null) return null;
        return masteryData.getDataForLevel(level);
    }

    static void loadStats() throws JSONException, InstantiationException, IllegalAccessException,
                                   ClassNotFoundException, IOException {
        JSONArray statsList = Global.getSettings().getMergedSpreadsheetData("id", "data/shipmastery/stats_list.csv");
        for (int i = 0; i < statsList.length(); i++) {
            JSONObject item = statsList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("location");
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            ShipStat stat = (ShipStat) cls.newInstance();
            stat.id = id;
            stat.description = item.getString("description");
            stat.tier = item.optInt("tier", 1);
            stat.defaultAmount = (float) item.optDouble("default_amount", 1f);
            stat.tags.addAll(Arrays.asList(item.getString("tags").trim().split("\\s+")));
            statSingletonMap.put(id, stat);
        }
    }

    public static void loadMasteryData()
            throws JSONException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        loadMasteries();
        loadStats();
        initMasteries();
    }

    public static MasteryGenerator makeGenerator(String rawString) {
        String[] strList = rawString.trim().split("\\s+");
        String id = strList[0];
        String[] params = Arrays.copyOfRange(strList, 1, strList.length);
        MasteryInfo info = masteryInfoMap.get(id);
        if (info == null) {
            throw new RuntimeException("Unknown mastery effect: " + id);
        }
        return new MasteryGenerator(info.effectClass, params, info.tags, info.defaultStrength, info.priority);
    }

    public static void processLevelData(Object generator, MasteryLevelData data, boolean isOption2)
            throws JSONException {
        if (generator instanceof String) {
            if (isOption2) {
                data.addGeneratorToOption2(makeGenerator((String) generator));
            } else {
                data.addGeneratorToOption1(makeGenerator((String) generator));
            }
        }
        // Array of strings means no options, just a list of mastery effects
        else if (generator instanceof JSONArray) {
            JSONArray array = (JSONArray) generator;
            if (array.length() == 0) return;
            for (int i = 0; i < array.length(); i++) {
                try {
                    String effectString = array.getString(i);
                    if (isOption2) {
                        data.addGeneratorToOption2(makeGenerator(effectString));
                    } else {
                        data.addGeneratorToOption1(makeGenerator(effectString));
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(array.getString(i) + " is not a string");
                }
            }
        }
        else if (generator instanceof JSONObject) {
            JSONObject json = (JSONObject) generator;
            processLevelData(json.get("A"), data, false);
            processLevelData(json.get("B"), data, true);
        }
        else {
            throw new RuntimeException("Unable to parse generator: " + generator);
        }
    }

    public static Pair<Integer, Map<Integer, MasteryLevelData>> initMasteries(String name, Set<String> presetChain) throws JSONException {
        Map<Integer, MasteryLevelData> levelDataMap = new HashMap<>();
        Integer maxLevel = null;
        JSONObject json = (JSONObject) masteryAssignments.opt(name);
        presetChain.add(name);

        if (json != null) {
            if (json.has("levels")) {
                JSONObject levels = (JSONObject) json.get("levels");
                Iterator<String> itr = levels.keys();
                while (itr.hasNext()) {
                    String levelStr = itr.next();
                    int level = Integer.parseInt(levelStr);
                    MasteryLevelData levelData = new MasteryLevelData(name, level);
                    processLevelData(levels.get(levelStr), levelData, false);
                    levelDataMap.put(level, levelData);
                }
            }
            if (json.has("maxLevel")) {
                maxLevel = json.getInt("maxLevel");
            }
        }

        String preset = json == null ? null : json.optString("preset", null);
        String defaultPreset = DEFAULT_PRESET_NAME;

        if (preset == null && !name.equals(defaultPreset)) {
            preset = defaultPreset;
        }
        if (presetChain.contains(preset)) {
            throw new RuntimeException("Circular preset dependency: " + presetChain);
        }
        if (preset != null) {
            Pair<Integer, Map<Integer, MasteryLevelData>> presetLevelData = initMasteries(preset, presetChain);
            maxLevel = maxLevel == null ? presetLevelData.one : Math.max(maxLevel, presetLevelData.one);
            for (Map.Entry<Integer, MasteryLevelData> entry : presetLevelData.two.entrySet()) {
                int level = entry.getKey();
                MasteryLevelData levelData = entry.getValue();
                if (!levelDataMap.containsKey(level)) {
                    MasteryLevelData copy = new MasteryLevelData(name, level);
                    for (MasteryGenerator generator : levelData.getGeneratorsOption1()) {
                        copy.addGeneratorToOption1(generator);
                    }
                    for (MasteryGenerator generator : levelData.getGeneratorsOption2()) {
                        copy.addGeneratorToOption2(generator);
                    }
                    levelDataMap.put(level, copy);
                }
            }
        }

        int ml = maxLevel == null ? 0 : maxLevel;
        HullMasteryData masteryData = new HullMasteryData(name, ml);
        for (int i = 1; i <= ml; i++) {
            masteryData.setLevelData(i, levelDataMap.get(i));
        }
        masteryMap.put(name, masteryData);
        return new Pair<>(maxLevel, levelDataMap);
    }


    public static void initMasteries() throws JSONException, IOException {
        masteryMap.clear();
        masteryAssignments = Global.getSettings().getMergedJSON("data/shipmastery/mastery_assignments.json");
        JSONObject presets = Global.getSettings().getMergedJSON("data/shipmastery/mastery_presets.json");
        Iterator<String> itr = presets.keys();
        while (itr.hasNext()) {
            String name = itr.next();
            masteryAssignments.put(name, presets.getJSONObject(name));
        }

        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            spec = Utils.getRestoredHullSpec(spec);
            String id = spec.getHullId();
            if (!masteryMap.containsKey(id)) {
                initMasteries(id, new HashSet<String>());
            }
        }
    }

    static void loadMasteries() throws JSONException, IOException, ClassNotFoundException {
        JSONArray masteryList =
                Global.getSettings().getMergedSpreadsheetData("id", "data/shipmastery/mastery_list.csv");
        for (int i = 0; i < masteryList.length(); i++) {
            JSONObject item = masteryList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("script");
            String tags = item.getString("tags");
            int tier = item.optInt("tier", 1);
            int priority = item.optInt("priority", 0);
            float defaultStrength = (float) item.optDouble("default_strength", 0f);
            Class<? extends MasteryEffect> cls =
                    (Class<? extends MasteryEffect>) Global.getSettings().getScriptClassLoader().loadClass(className);

            MasteryInfo info = new MasteryInfo();
            effectToIdMap.put(cls, id);
            info.defaultStrength = defaultStrength;
            info.effectClass = cls;
            info.priority = priority;
            info.tier = tier;
            info.tags = new HashSet<>();
            info.tags.addAll(Arrays.asList(tags.trim().split("\\s+")));
            masteryInfoMap.put(id, info);
        }
    }

    public static void generateMasteries(ShipHullSpecAPI spec) throws InstantiationException, IllegalAccessException {
        HullMasteryData data = masteryMap.get(spec.getHullId());
        for (int i = 1; i <= data.getMaxLevel(); i++) {
            MasteryLevelData levelData = data.getDataForLevel(i);
            if (levelData != null) {
                levelData.generateEffects();
            }
        }
    }

    public static void generateMasteries() throws InstantiationException, IllegalAccessException {
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            ShipHullSpecAPI restoredSpec = Utils.getRestoredHullSpec(spec);
            if (spec != restoredSpec) continue;

            generateMasteries(spec);
            clearInvalidActiveLevels(spec);

            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSector().getPlayerPerson(), spec, new MasteryUtils.MasteryAction() {
                        @Override
                        public void perform(MasteryEffect effect) {
                            effect.onActivate(Global.getSector().getPlayerPerson());
                        }
                    });
        }
    }

    static void clearInvalidActiveLevels(ShipHullSpecAPI spec) {
        spec = Utils.getRestoredHullSpec(spec);
        SaveData data = SAVE_DATA_TABLE.get(spec.getHullId());
        if (data == null) return;
        Iterator<Map.Entry<Integer, Boolean>> itr = data.activeLevels.entrySet().iterator();
        while (itr.hasNext()) {
            if (itr.next().getKey() > getMaxMasteryLevel(spec)) {
                itr.remove();
            }
        }
    }

    public static void loadMasteryTable() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        if (!persistentData.containsKey(MASTERY_KEY)) {
            SAVE_DATA_TABLE = new SaveDataTable();
            persistentData.put(MASTERY_KEY, SAVE_DATA_TABLE);
        } else {
            SAVE_DATA_TABLE = (SaveDataTable) persistentData.get(MASTERY_KEY);
        }
    }

    public static String getId(Class<?> effectClass) {
        return effectToIdMap.get(effectClass);
    }

    public static ShipStat getStatParams(String id) {
        return statSingletonMap.get(id);
    }

    public static Set<String> getAllStatNames() {
        return statSingletonMap.keySet();
    }

    public static Set<String> getAllMasteryNames() {
        return masteryInfoMap.keySet();
    }

    public static MasteryInfo getMasteryInfo(String name) {
        return masteryInfoMap.get(name);
    }
}
