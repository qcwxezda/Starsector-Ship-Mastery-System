package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.data.HullMasteryData;
import shipmastery.data.MasteryLevelData;
import shipmastery.data.SaveData;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryEffect;
import shipmastery.stats.ShipStat;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.io.IOException;
import java.util.*;

public abstract class ShipMastery {

    /**
     * Maps base hull spec ids to structure containing mastery data for that hull spec
     */
    public static class SaveDataTable extends HashMap<String, SaveData> {}

    public static final String MASTERY_KEY = "shipmastery_Mastery";
    public static final String DEFAULT_PRESET_NAME = "default";
    private static SaveDataTable SAVE_DATA_TABLE;

    /**
     * Maps mastery effect classes to their ids
     */
    private static final Map<Class<?>, String> effectToIdMap = new HashMap<>();
    /**
     * Maps mastery effect ids to their classes
     */
    private static final Map<String, Class<?>> idToEffectMap = new HashMap<>();
    /**
     * Preset name -> Mastery level -> initialization strings
     */
    private static final Utils.ListPairMapMap<String, Integer, String> assignmentsMap = new Utils.ListPairMapMap<>();
    private static final Map<String, Integer> maxLevelMap = new HashMap<>();
    /** For each hull type, keep track of the preset that should be used if a level isn't specified. */
    private static final Map<String, String> assignmentsToPresetsMap = new HashMap<>();
    private static final Utils.ListPairMapMap<String, Integer, String> presetsMap = new Utils.ListPairMapMap<>();
    private static final Map<String, Integer> presetsMaxLevelMap = new HashMap<>();

    /**
     * Ship stat id -> singleton object
     */
    private static final Map<String, ShipStat> statSingletonMap = new HashMap<>();
    private static final Map<String, HullMasteryData> masteryMap = new HashMap<>();


    public static int getPlayerMaxMastery(ShipHullSpecAPI spec) {
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
     * This function 1-indexed
     */
    public static List<MasteryEffect> getMasteryEffects(ShipHullSpecAPI spec, int level, boolean isOption2) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData masteryData = masteryMap.get(id);
        if (masteryData == null) return new ArrayList<>();
        MasteryLevelData levelData = masteryData.getDataForLevel(level - 1);
        if (levelData == null) return new ArrayList<>();
        return isOption2 ? levelData.getEffectsListOption2() : levelData.getEffectsListOption1();
    }

    public static List<MasteryEffect> getMasteryEffectsBothOptions(ShipHullSpecAPI spec, int level) {
        List<MasteryEffect> masteryEffects = new ArrayList<>();
        masteryEffects.addAll(getMasteryEffects(spec, level, false));
        masteryEffects.addAll(getMasteryEffects(spec, level, true));
        return masteryEffects;
    }

    static Map<String, String> tagMap = new HashMap<>();
    static Map<String, Integer> tierMap = new HashMap<>();
    static Map<String, Integer> priorityMap = new HashMap<>();
    static Map<String, Float> defaultStrengthMap = new HashMap<>();

    static void parseLevelItem(String key, int level, Object levelItem,
                               Utils.ListPairMapMap<String, Integer, String> mapToAddTo, boolean isOption2) {
        if (levelItem instanceof String) {
            if (isOption2) {
                mapToAddTo.add2(key, level, (String) levelItem);
            } else {
                mapToAddTo.add1(key, level, (String) levelItem);
            }
        }
        // Array of strings means no options, just a list of mastery effects
        else if (levelItem instanceof JSONArray) {
            JSONArray levelItemArray = (JSONArray) levelItem;
            if (levelItemArray.length() == 0) return;
            for (int i = 0; i < levelItemArray.length(); i++) {
                try {
                    String effectString = levelItemArray.getString(i);
                    if (isOption2) {
                        mapToAddTo.add2(key, level, effectString);
                    } else {
                        mapToAddTo.add1(key, level, effectString);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException("Effect initializer not a string: " + key + " at level " + level);
                }
            }
        } else {
            throw new RuntimeException("Malformed level item: " + key + " at level " + level);
        }
    }

    static void parseLevelItemOrOption(
            String key,
            String levelString,
            JSONObject levelsJson,
            Utils.ListPairMapMap<String, Integer, String> mapToAddTo,
            Map<String, Integer> mapToCheckLevels) throws JSONException {
        int level;
        try {
            level = Integer.parseInt(levelString);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Level key " + levelString + " in preset " + key + " is not a number");
        }

        Integer maxLevel = mapToCheckLevels.get(key);
        if (maxLevel == null || level > maxLevel) {
            throw new RuntimeException(
                    "Assignment or preset " + key + " has max level " + maxLevel + "; can't assign mastery to level " +
                            level);
        }

        Object levelItem = levelsJson.get(levelString);
        if (levelItem instanceof String || levelItem instanceof JSONArray) {
            parseLevelItem(key, level, levelItem, mapToAddTo, false);
        }
        // JSON object means it's a choice between two effect lists
        else if (levelItem instanceof JSONObject) {
            JSONObject levelItemObject = (JSONObject) levelItem;
            if (!levelItemObject.has("A")) {
                throw new RuntimeException("Option A missing from level " + level + " of preset " + key);
            }
            if (!levelItemObject.has("B")) {
                throw new RuntimeException("Option B missing from level " + level + "of preset " + key);
            }
            parseLevelItem(key, level, levelItemObject.get("A"), mapToAddTo, false);
            parseLevelItem(key, level, levelItemObject.get("B"), mapToAddTo, true);
        }
    }

    static void loadPresets()
            throws JSONException, IOException {
        JSONObject masteryPresets = Global.getSettings().getMergedJSON("data/shipmastery/mastery_presets.json");

        //noinspection unchecked
        Iterator<String> presetsIterator = masteryPresets.keys();
        while (presetsIterator.hasNext()) {
            String presetName = presetsIterator.next();
            JSONObject preset = masteryPresets.getJSONObject(presetName);
            int maxLevel = preset.optInt("maxLevel", 0);
            ShipMastery.presetsMaxLevelMap.put(presetName, maxLevel);
            JSONObject levelsJson = preset.optJSONObject("levels");
            if (levelsJson != null) {
                //noinspection unchecked
                Iterator<String> levelsItr = levelsJson.keys();
                while (levelsItr.hasNext()) {
                    parseLevelItemOrOption(presetName, levelsItr.next(), levelsJson, ShipMastery.presetsMap,
                                           ShipMastery.presetsMaxLevelMap);
                }
            }
        }
    }

    static void loadAssignments() throws JSONException, IOException {
        JSONObject masteryAssignments = Global.getSettings().getMergedJSON("data/shipmastery/mastery_assignments.json");
        //noinspection unchecked
        Iterator<String> assignmentsIterator = masteryAssignments.keys();
        while (assignmentsIterator.hasNext()) {
            String hullId = assignmentsIterator.next();
            JSONObject assignment = masteryAssignments.getJSONObject(hullId);
            int maxLevel;
            String presetName = assignment.optString("preset", null);
            if (presetName != null) {
                assignmentsToPresetsMap.put(hullId, presetName);
            }
            else {
                assignmentsToPresetsMap.put(hullId, DEFAULT_PRESET_NAME);
            }
            if (assignment.has("maxLevel")) {
                maxLevel = assignment.getInt("maxLevel");
            }
            else if (presetName != null && presetsMaxLevelMap.containsKey(presetName)) {
                maxLevel = presetsMaxLevelMap.get(presetName);
            }
            else {
                maxLevel = presetsMaxLevelMap.get(DEFAULT_PRESET_NAME);
            }
            maxLevelMap.put(hullId, maxLevel);
            JSONObject levelsJson = assignment.getJSONObject("levels");
            //noinspection unchecked
            Iterator<String> levelsItr = levelsJson.keys();
            while (levelsItr.hasNext()) {
                parseLevelItemOrOption(hullId, levelsItr.next(), levelsJson, assignmentsMap, maxLevelMap);
            }
        }
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
            stat.name = item.getString("name");
            stat.tier = item.optInt("tier", 1);
            stat.defaultAmount = (float) item.optDouble("default_amount", 1f);
            stat.tags.addAll(Arrays.asList(item.getString("tags").trim().split("\\s+")));
            statSingletonMap.put(id, stat);
        }
    }

    static void loadMasteriesCsv() throws JSONException, IOException, ClassNotFoundException {
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
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            effectToIdMap.put(cls, id);
            idToEffectMap.put(id, cls);
            tagMap.put(id, tags);
            tierMap.put(id, tier);
            priorityMap.put(id, priority);
            defaultStrengthMap.put(id, defaultStrength);
        }
    }

    static MasteryEffect instantiateEffect(String generator, ShipHullSpecAPI hullSpec, int level, int index) throws InstantiationException, IllegalAccessException {
        String[] params = generator.trim().split("\\s+");
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
        effect.setPriority(priorityMap.get(id));
        effect.setHullSpec(hullSpec);
        effect.setId(MasteryUtils.makeEffectId(effect, level, index));
        effect.addTags(tagMap.get(id).trim().split("\\s+"));
        effect.init(Arrays.copyOfRange(params, 1, params.length));
        return effect;
    }

    public static void generateMasteries(ShipHullSpecAPI spec) throws InstantiationException, IllegalAccessException {
        ShipHullSpecAPI restoredSpec = Utils.getRestoredHullSpec(spec);
        String restoredSpecId = restoredSpec.getHullId();
        Integer maxLevel = maxLevelMap.get(restoredSpecId);
        SortedMap<Integer, Pair<List<String>, List<String>>> masteries;
        // Hull not tracked, so use the default preset
        if (maxLevel == null) {
            maxLevel = presetsMaxLevelMap.get(DEFAULT_PRESET_NAME);
            masteries = presetsMap.get(DEFAULT_PRESET_NAME);
        }
        else {
            masteries = assignmentsMap.get(restoredSpecId);
        }

        HullMasteryData masteryData = new HullMasteryData(restoredSpec);
        for (int i = 1; i <= maxLevel; i++) {
            MasteryLevelData levelData = new MasteryLevelData(restoredSpec, i);
            Pair<List<String>, List<String>> masteryOptions = null;
            // Mastery assignments contains an entry, so use that
            if (masteries.containsKey(i)) {
                masteryOptions = masteries.get(i);
            }
            // Mastery assignments doesn't contain the entry, but the preset does, so use that
            // as a fallback
            else if (assignmentsToPresetsMap.containsKey(restoredSpecId)) {
                masteryOptions = presetsMap.get(assignmentsToPresetsMap.get(restoredSpecId)).get(i);
            }
            // Neither the assignments nor the preset have data for this level, so need to randomly generate it
            if (masteryOptions == null) {
                masteryOptions = new Pair<List<String>, List<String>>(new ArrayList<String>(), new ArrayList<String>());
                masteryOptions.one.add("ModifyStatsMult 1 FluxCapacity");
            }
            List<String> one = masteryOptions.one;
            for (int j = 0; j < one.size(); j++) {
                String generator = one.get(j);
                levelData.addEffectToOption1(instantiateEffect(generator, restoredSpec, i, j));
            }
            List<String> two = masteryOptions.two;
            for (int j = 0; j < two.size(); j++) {
                String generator = two.get(j);
                levelData.addEffectToOption2(instantiateEffect(generator, restoredSpec, i, j));
            }
            masteryData.addLevelData(levelData);
        }
        masteryMap.put(restoredSpecId, masteryData);
    }

    public static void loadMasteryData()
            throws JSONException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        loadMasteriesCsv();
        loadStats();
        loadPresets();
        loadAssignments();
    }

    public static void generateAllMasteries() throws InstantiationException, IllegalAccessException {
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            spec = Utils.getRestoredHullSpec(spec);

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
            if (itr.next().getKey() > getPlayerMaxMastery(spec)) {
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


}
