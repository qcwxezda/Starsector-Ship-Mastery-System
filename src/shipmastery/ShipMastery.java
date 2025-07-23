package shipmastery;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import shipmastery.achievements.LevelUp;
import shipmastery.achievements.MasteredMany;
import shipmastery.achievements.MaxLevel;
import shipmastery.achievements.UnlockAchievementAction;
import shipmastery.aicoreinterface.AICoreInterfacePlugin;
import shipmastery.campaign.PlayerMPHandler;
import shipmastery.campaign.listeners.PlayerGainedMPListenerHandler;
import shipmastery.data.HullMasteryData;
import shipmastery.data.MasteryGenerator;
import shipmastery.data.MasteryInfo;
import shipmastery.data.MasteryLevelData;
import shipmastery.data.SaveData;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.preset.PresetCheckScript;
import shipmastery.plugin.ModPlugin;
import shipmastery.stats.ShipStat;
import shipmastery.util.IntRef;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class ShipMastery {

    public static final String REROLL_SEQUENCE_MAP = "$sms_RerollMapV3";

    /**
     * Maps base hull spec ids to structure containing mastery data for that hull spec
     */
    public static class SaveDataTable extends HashMap<String, SaveData> {}

    public static final String MASTERY_KEY = "shipmastery_Mastery";
    public static final String DEFAULT_PRESET_NAME = "_DEFAULT_";
    public static final String PRESET_CHECK_KEY = "presetCheckScript";
    private static SaveDataTable SAVE_DATA_TABLE;

    /**
     * Ship stat id -> singleton object
     */
    private static final Map<String, ShipStat> statSingletonMap = new HashMap<>();
    private static final Map<String, AICoreInterfacePlugin> aiCoreInterfaceSingletonMap = new HashMap<>();
    private static final Map<Class<?>, String> effectToIdMap = new HashMap<>();
    private static final Map<String, Map<String, Float>> selectionWeightMap = new HashMap<>();

    /** Keep track of all rerolled ships this save, as their mastery effects must be recreated on game load. */
    private static final Set<String> rerolledSpecs = new HashSet<>();

    private static final Map<String, PresetCheckScript> presetNameToCheckerMap = new HashMap<>();
    private static final Map<String, HullMasteryData> masteryMap = new HashMap<>();
    private static JSONObject masteryAssignments;
    private static final Map<String, MasteryInfo> masteryInfoMap = new HashMap<>();
    private static final Map<String, String> masteryAliasMap = new HashMap<>();

    public static void addRerolledSpecThisSave(ShipHullSpecAPI spec) {
        rerolledSpecs.add(spec.getHullId());
    }

    public static void clearRerolledSpecsThisSave() {
        rerolledSpecs.clear();
    }

    public static int getMaxMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData data = masteryMap.get(id);
        return data == null ? 0 : data.getMaxLevel();
    }

    public static int getPlayerMasteryLevel(ShipHullSpecAPI spec) {
        if (SAVE_DATA_TABLE == null) return 0;

        SaveData data = SAVE_DATA_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? 0 : Math.min(data.level, getMaxMasteryLevel(spec));
    }

    public static void advancePlayerMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);

        if (data == null) {
            data = new SaveData(0, 1);
            SAVE_DATA_TABLE.put(id, data);
        } else {
            if (data.level >= getMaxMasteryLevel(spec)) return;
            data.level++;
        }

        List<String> optionIds = getMasteryOptionIds(spec, data.level);
        if (optionIds.size() == 1) {
            boolean autoActivate = true;
            String optionId = optionIds.get(0);
            for (MasteryEffect effect : getMasteryEffects(spec, data.level, optionId)) {
                if (!MasteryUtils.isAutoActivate(effect)) {
                    autoActivate = false;
                    break;
                }
            }
            if (autoActivate) {
                activatePlayerMastery(spec, data.level, optionId);
            }
        }

        UnlockAchievementAction.unlockWhenUnpaused(LevelUp.class);

        if (getPlayerMasteryLevel(spec) >= getMaxMasteryLevel(spec)) {
            MasteredMany.refreshPlayerMasteredCount();
            UnlockAchievementAction.unlockWhenUnpaused(MaxLevel.class);
            Integer count = (Integer) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get(MasteredMany.MASTERED_COUNT_KEY);
            if (count != null && count >= MasteredMany.NUM_NEEDED) {
                UnlockAchievementAction.unlockWhenUnpaused(MasteredMany.class);
            }
        }
    }

    public static float getPlayerMasteryPoints(ShipHullSpecAPI spec) {
        if (SAVE_DATA_TABLE == null) return 0f;

        SaveData data = SAVE_DATA_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? 0 : data.points;
    }

    public enum MasteryGainSource {
        COMBAT,
        NONCOMBAT,
        ITEM,
        TRICKLE,
        OTHER
    }

    public static void addPlayerMasteryPoints(
            ShipHullSpecAPI spec,
            float amount,
            boolean trickleToSkins,
            boolean countsForDifficultyProgression,
            MasteryGainSource source) {
        ShipHullSpecAPI restored = Utils.getRestoredHullSpec(spec);
        String baseHullId = restored.getBaseHullId();
        Set<String> allSkins = Utils.baseHullToAllSkinsMap.getOrDefault(baseHullId, new HashSet<>());

        String restoredId = restored.getHullId();
        List<Pair<String, Float>> toAdd = new ArrayList<>();
        toAdd.add(new Pair<>(restoredId, 1f));

        if (trickleToSkins) {
            for (String skinId : allSkins) {
                if (Objects.equals(restoredId, skinId)) continue;
                toAdd.add(new Pair<>(skinId, 0.5f));
            }
        }

        amount = PlayerGainedMPListenerHandler.modifyPlayerMPGain(spec, amount, source);

        if (countsForDifficultyProgression) {
            PlayerMPHandler.addTotalCombatMP(amount);
        }

        // Gaining mastery points for a ship type also gains mastery points for all skins that share the same
        // base ship; the other skins will gain half the mastery points.
        for (Pair<String, Float> elem : toAdd) {
            SaveData data = SAVE_DATA_TABLE.get(elem.one);
            if (data == null) {
                SAVE_DATA_TABLE.put(elem.one, new SaveData(amount * elem.two, 0));
            } else {
                data.points += amount * elem.two;
            }
        }

        PlayerGainedMPListenerHandler.reportPlayerMPGain(spec, amount, source);
    }

    public static void spendPlayerMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.get(id);
        if (data == null) return;

        data.points -= amount;
        data.points = Math.max(0f, data.points);
    }

    public static void activatePlayerMastery(ShipHullSpecAPI spec, int level, String optionId) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.computeIfAbsent(id, k -> new SaveData(0, 0));

        if (!data.activateLevel(level, optionId)) return;
        List<MasteryEffect> effects = getMasteryEffects(spec, level, optionId);
        for (MasteryEffect effect : effects) {
            effect.onActivate(Global.getSector().getPlayerPerson());
        }
        DeferredActionPlugin.performLater(Utils::fixPlayerFleetInconsistencies, 0f);
    }

    public static void deactivatePlayerMastery(ShipHullSpecAPI spec, int level, String optionId) {
        String id = Utils.getRestoredHullSpecId(spec);
        SaveData data = SAVE_DATA_TABLE.computeIfAbsent(id, k -> new SaveData(0, 0));

        if (!data.deactivateLevel(level)) return;
        List<MasteryEffect> effects = getMasteryEffects(spec, level, optionId);
        for (MasteryEffect effect : effects) {
            effect.onDeactivate(Global.getSector().getPlayerPerson());
        }
        DeferredActionPlugin.performLater(Utils::fixPlayerFleetInconsistencies, 0f);
    }

    /**
     * Returns a copy of the original data
     */
    public static NavigableMap<Integer, String> getPlayerActiveMasteriesCopy(ShipHullSpecAPI spec) {
        if (SAVE_DATA_TABLE == null || spec == null) return new TreeMap<>();

        SaveData data = SAVE_DATA_TABLE.get(Utils.getRestoredHullSpecId(spec));
        return data == null ? new TreeMap<>() : new TreeMap<>(data.activeLevels);
    }

    public static List<String> getMasteryOptionIds(ShipHullSpecAPI spec, int level) {
        MasteryLevelData levelData = getLevelData(spec, level);
        if (levelData == null) return new ArrayList<>();
        return new ArrayList<>(levelData.getGeneratorsLists().keySet());
    }

    /**
     * This function is 1-indexed
     */
    public static List<MasteryEffect> getMasteryEffects(ShipHullSpecAPI spec, int level, String optionId) {
        MasteryLevelData levelData = getLevelData(spec, level);
        if (levelData == null) return new ArrayList<>();
        var res = levelData.getEffectsLists().get(optionId);
        return res == null ? new ArrayList<>() : res;
    }

    public static List<MasteryGenerator> getGenerators(ShipHullSpecAPI spec, int level, String optionId) {
        MasteryLevelData levelData = getLevelData(spec, level);
        if (levelData == null) return new ArrayList<>();
        var res = levelData.getGeneratorsLists().get(optionId);
        return res == null ? new ArrayList<>() : res;
    }

    private static MasteryLevelData getLevelData(ShipHullSpecAPI spec, int level) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData masteryData = masteryMap.get(id);
        if (masteryData == null) return null;
        generateIfNeeded(spec);
        return masteryData.getDataForLevel(level);
    }

    // prevent infinite recursion
    private static boolean shouldGenerate = true;
    public static void generateIfNeeded(ShipHullSpecAPI spec) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData masteryData = masteryMap.get(id);
        if (masteryData == null) return;
        // Don't generate if so early in the load process that the persistent data hasn't been loaded yet
        Boolean b = (Boolean) Global.getSector().getPersistentData().get(ModPlugin.RANDOM_MODE_KEY);
        if (b == null) return;
        if (!masteryData.isGenerated() && shouldGenerate) {
            try {
                shouldGenerate = false;
                generateMasteries(spec);
                masteryData.setGenerated(true);
                shouldGenerate = true;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static MasteryLevelData getLevelDataNoGenerate(ShipHullSpecAPI spec, int level) {
        String id = Utils.getRestoredHullSpecId(spec);
        HullMasteryData masteryData = masteryMap.get(id);
        if (masteryData == null) return null;
        return masteryData.getDataForLevel(level);
    }

    public static void loadStats() throws NoSuchMethodException, IllegalAccessException, JSONException, IOException, ClassNotFoundException {
        JSONArray statsList = Global.getSettings().getMergedSpreadsheetData("id", "data/shipmastery/stats_list.csv");
        for (int i = 0; i < statsList.length(); i++) {
            JSONObject item = statsList.getJSONObject(i);
            String id = item.getString("id");
            String className = item.getString("location");
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            ShipStat stat = (ShipStat) Utils.instantiateClassNoParams(cls);
            stat.id = id;
            stat.description = item.getString("description");
            stat.tier = item.optInt("tier", 1);
            stat.defaultAmount = (float) item.optDouble("default_amount", 1f);
            stat.tags.addAll(Arrays.asList(item.getString("tags").trim().split("\\s+")));
            statSingletonMap.put(id, stat);
        }
    }

    public static AICoreInterfacePlugin getAICoreInterfacePlugin(String coreId) {
        return aiCoreInterfaceSingletonMap.get(coreId);
    }

    public static Map<String, AICoreInterfacePlugin> getAICoreInterfaceSingletonMap() {
        return Collections.unmodifiableMap(aiCoreInterfaceSingletonMap);
    }

    public static void loadAICoreInterfaces() throws NoSuchMethodException, IllegalAccessException, JSONException, IOException, ClassNotFoundException, InstantiationException {
        JSONArray interfaceList = Global.getSettings().getMergedSpreadsheetData("commodity_id", "data/shipmastery/ai_core_interface_list.csv");
        for (int i = 0; i < interfaceList.length(); i++) {
            JSONObject item = interfaceList.getJSONObject(i);
            String commodityId = item.getString("commodity_id");
            String className = item.getString("plugin");
            Class<?> cls = Global.getSettings().getScriptClassLoader().loadClass(className);
            var plugin = Utils.instantiateClassNoParams(cls);
            if (!(plugin instanceof AICoreInterfacePlugin p)) {
                throw new InstantiationException("plugin "
                        + className
                        +  " in ai_core_interface_list.csv must implement shipmastery.aicoreinterface.AICoreInterfacePlugin");
            }
            aiCoreInterfaceSingletonMap.put(commodityId, p);
        }
    }

    public static MasteryGenerator makeGenerator(String rawString) {
        String[] strList = rawString.trim().split("\\s+");
        String id = strList[0];
        String[] params = Arrays.copyOfRange(strList, 1, strList.length);
        MasteryInfo info = masteryInfoMap.get(id);
        if (info == null) {
            throw new RuntimeException("Unknown mastery effect: " + id);
        }
        return new MasteryGenerator(info, params);
    }

    public static void processLevelData(Object generator, MasteryLevelData data, String optionId)
            throws JSONException {
        if (generator instanceof String) {
            data.addGeneratorToList(optionId, makeGenerator((String) generator));
        }
        // Array of strings means no options, just a list of mastery effects
        else if (generator instanceof JSONArray array) {
            if (array.length() == 0) return;
            for (int i = 0; i < array.length(); i++) {
                try {
                    String effectString = array.getString(i);
                    data.addGeneratorToList(optionId, makeGenerator(effectString));
                } catch (JSONException e) {
                    throw new RuntimeException(array.getString(i) + " is not a string");
                }
            }
        }
        else if (generator instanceof JSONObject json) {
            for (var it = json.keys(); it.hasNext(); ) {
                if (!(it.next() instanceof String key)) continue;
                processLevelData(json.get(key), data, key);
            }
        }
        else {
            throw new RuntimeException("Unable to parse generator: " + generator);
        }
    }

    /** The boolean in the pair is True if the level was directly set and False if the level was propagated from a preset. */
    public static Map<Integer, Pair<MasteryLevelData, Boolean>> initMasteries(
            String name,
            Set<String> presetChain,
            Map<String, Map<Integer, Pair<MasteryLevelData, Boolean>>> cachedPresetData,
            IntRef savedMaxLevel) throws JSONException {
        var cached = cachedPresetData.get(name);
        if (cached != null) {
            return cached;
        }

        if (presetChain.contains(name)) {
            throw new RuntimeException("Circular mastery preset dependency: " + presetChain + " -> " + name);
        }

        Map<Integer, Pair<MasteryLevelData, Boolean>> levelDataMap = new HashMap<>();
        Integer maxLevel = null;
        JSONObject json = (JSONObject) masteryAssignments.opt(name);
        presetChain.add(name);

        if (json != null && json.has("maxLevel")) {
            maxLevel = json.getInt("maxLevel");
        }

        Object presetStringOrArray = json == null ? null : json.opt("preset");
        List<String> presets = new ArrayList<>();
        if (presetStringOrArray != null) {
            if (presetStringOrArray instanceof String str) {
                presets.add(str);
            } else {
                JSONArray jsonArray  = (JSONArray) presetStringOrArray;
                for (int i = 0; i < jsonArray.length(); i++) {
                    presets.add(jsonArray.getString(i));
                }
            }
        }

        // No declared presets
        // If itself a preset, then use _DEFAULT_
        // If a hull spec, then check all possible presets
        if (presets.isEmpty() && !Objects.equals(name, DEFAULT_PRESET_NAME)) {
            if (Utils.allHullSpecIds.contains(name)) {
                List<Pair<String, Float>> presetsWithScore = new ArrayList<>();
                for (var entry : presetNameToCheckerMap.entrySet()) {
                    if (entry.getValue() == null) continue;
                    float score = entry.getValue().computeScore(Global.getSettings().getHullSpec(name));
                    if (score > 0f) {
                        presetsWithScore.add(new Pair<>(entry.getKey(), score));
                    }
                }
                presetsWithScore.sort(Comparator.comparing((Function<Pair<String, Float>, Float>) (a -> a.two)).reversed());
                for (var pair : presetsWithScore) {
                    presets.add(pair.one);
                }
            }
        }

        // Still empty, use default spec
        if (presets.isEmpty() && !Objects.equals(name, DEFAULT_PRESET_NAME)) {
            presets.add(DEFAULT_PRESET_NAME);
        }

        Map<String, Map<Integer, Pair<MasteryLevelData, Boolean>>> presetLevelData = new LinkedHashMap<>();
        for (String preset : presets) {
            // Check that the preset is actually a preset
            if (!presetNameToCheckerMap.containsKey(preset)) {
                throw new RuntimeException("Unknown mastery preset: " + preset);
            }
            var data = initMasteries(preset, presetChain, cachedPresetData, savedMaxLevel);
            presetLevelData.put(preset, data);
            if (maxLevel == null) {
                maxLevel = savedMaxLevel.value;
            }
        }

        int ml = maxLevel == null ? 0 : maxLevel;
        if (json != null && json.has("levels")) {
            JSONObject levels = (JSONObject) json.get("levels");
            Iterator<String> itr = levels.keys();
            while (itr.hasNext()) {
                String levelStr = itr.next();
                int level;
                if ("max".equals(levelStr.toLowerCase(Locale.ROOT))) {
                    level = ml;
                } else {
                    level = Integer.parseInt(levelStr);
                }
                MasteryLevelData levelData = new MasteryLevelData(name, level);
                processLevelData(levels.get(levelStr), levelData, "");
                levelDataMap.put(level, new Pair<>(levelData, true));
            }
        }

        for (int i = 1; i <= ml; i++) {
            if (levelDataMap.containsKey(i)) continue;
            for (var presetEntry : presetLevelData.entrySet()) {
                var data = presetEntry.getValue();
                var levelData = data.get(i);
                if (levelData == null) continue;
                if (levelData.two) {
                    copyLevelGenerators(name, levelDataMap, i, levelData);
                    break;
                }
            }
            // No preset matches, use _DEFAULT_
            if (!levelDataMap.containsKey(i)) {
                var defaultData = cachedPresetData.get(DEFAULT_PRESET_NAME);
                if (defaultData == null) {
                    continue;
                }
                var levelData = defaultData.get(i);
                if (levelData == null) continue;
                if (levelData.two) {
                    copyLevelGenerators(name, levelDataMap, i, levelData);
                }
            }
        }

        HullMasteryData masteryData = new HullMasteryData(name, ml);
        MasteryInfo defaultInfo = getMasteryInfo("EmptyMastery");
        for (int i = 1; i <= ml; i++) {
            if (levelDataMap.get(i) == null || levelDataMap.get(i).one.getGeneratorsLists().isEmpty()) {
                MasteryLevelData data = new MasteryLevelData(name, i);
                data.addGeneratorToList("", new MasteryGenerator(defaultInfo, null));
                masteryData.setLevelData(i, data);
            } else {
                masteryData.setLevelData(i, levelDataMap.get(i).one);
            }
        }
        masteryMap.put(name, masteryData);
        savedMaxLevel.value = maxLevel;
        presetChain.remove(name);
        cachedPresetData.put(name, levelDataMap);
        return levelDataMap;
    }

    private static void copyLevelGenerators(
            String name,
            Map<Integer, Pair<MasteryLevelData, Boolean>> levelDataMap,
            int level,
            Pair<MasteryLevelData, Boolean> levelData) {
        MasteryLevelData copy = new MasteryLevelData(name, level);
        for (var entry2 : levelData.one.getGeneratorsLists().entrySet()) {
            for (MasteryGenerator generator : entry2.getValue()) {
                copy.addGeneratorToList(entry2.getKey(), generator);
            }
        }
        levelDataMap.put(level, new Pair<>(copy, false));
    }


    public static void initMasteries(boolean randomMode) throws JSONException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        masteryMap.clear();
        presetNameToCheckerMap.clear();

        JSONObject presets = Global.getSettings().getMergedJSON("data/shipmastery/mastery_presets.json");

        // Populate tag and built-in mod to default presets map
        Iterator<String> itr = presets.keys();
        while (itr.hasNext()) {
            String name = itr.next();
            JSONObject obj = presets.optJSONObject(name);
            if (obj == null) continue;
            if (obj.has(PRESET_CHECK_KEY)) {
                String className = obj.getString(PRESET_CHECK_KEY);
                presetNameToCheckerMap.put(name,
                        (PresetCheckScript) Utils.instantiateClassNoParams(Global.getSettings().getScriptClassLoader().loadClass(className)));
            } else {
                presetNameToCheckerMap.put(name, null);
            }
        }

        if (!randomMode) {
            masteryAssignments = Global.getSettings().getMergedJSON("data/shipmastery/mastery_assignments.json");
            itr = presets.keys();
            while (itr.hasNext()) {
                String name = itr.next();
                masteryAssignments.put(name, presets.getJSONObject(name));
            }

            for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
                spec = Utils.getRestoredHullSpec(spec);
                String id = spec.getHullId();
                if (!masteryMap.containsKey(id)) {
                    initMasteries(id, new LinkedHashSet<>(), new HashMap<>(), new IntRef());
                }
            }
        }
        else {
            JSONObject defaultPreset = presets.getJSONObject(DEFAULT_PRESET_NAME);
            int maxLevel = defaultPreset.getInt("maxLevel");
            List<Integer> allLevels = new ArrayList<>();
            for (int i = 1; i <= maxLevel; i++) {
                allLevels.add(i);
            }
            MasteryInfo sModCapacityInfo = getMasteryInfo("SModCapacityAsFractionOfMax");
            MasteryInfo randomInfo = getMasteryInfo("RandomMastery");
            for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
                spec = Utils.getRestoredHullSpec(spec);
                String id = spec.getHullId();
                if (!masteryMap.containsKey(id)) {
                    int seed = (id + "_" + MasteryUtils.getRandomMasterySeed()).hashCode();
                    HullMasteryData data = new HullMasteryData(id, maxLevel);
                    Collections.shuffle(allLevels, new Random(seed));
                    Set<Integer> sModLevels = new HashSet<>();
                    for (int i = 0; i < Math.min(2, maxLevel); i++) {
                        sModLevels.add(allLevels.get(i));
                    }
                    boolean shouldRoundUp = false;
                    for (int i = 1; i <= maxLevel; i++) {
                        MasteryLevelData levelData = new MasteryLevelData(id, i);
                        MasteryGenerator generator;
                        if (sModLevels.contains(i)) {
                            generator = new MasteryGenerator(sModCapacityInfo, new String[] {"0.5", shouldRoundUp ? "ROUND_UP" : "ROUND_DOWN"});
                            shouldRoundUp = true;
                        }
                        else {
                            generator = new MasteryGenerator(randomInfo, new String[] {"1", "9999999"});
                        }
                        levelData.addGeneratorToList("", generator);
                        data.setLevelData(i, levelData);
                    }
                    masteryMap.put(id, data);
                }
            }
        }
    }

    public static void loadAliases() throws JSONException, IOException {
        JSONObject aliases = Global.getSettings().getMergedJSON("data/shipmastery/mastery_aliases.json");
        for (Iterator<String> it = aliases.keys(); it.hasNext(); ) {
            String parent = it.next();
            JSONArray array = aliases.getJSONArray(parent);
            for (int i = 0; i < array.length(); i++) {
                String child = array.getString(i);
                masteryAliasMap.put(child, parent);
            }
        }
    }

    public static String getParentHullId(String child) {
        return masteryAliasMap.get(child);
    }

    public static void loadMasteries() throws JSONException, IOException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException {
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

            MasteryGenerator dummyGenerator = new MasteryGenerator(info,null);
            Map<String, Float> weights = new HashMap<>();
            for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
                if (spec != Utils.getRestoredHullSpec(spec)) continue;

                MasteryEffect dummy = dummyGenerator.generateDontInit(spec, 1, 0, "A");
                Float weight = dummy.getSelectionWeight(spec);
                weights.put(spec.getHullId(), weight);
            }
            selectionWeightMap.put(id, weights);
        }
    }

    public static void generateMasteries(ShipHullSpecAPI spec) throws InstantiationException, IllegalAccessException, NoSuchMethodException {
        spec = Utils.getRestoredHullSpec(spec);
        Set<Integer> levels = new HashSet<>();
        HullMasteryData data = masteryMap.get(spec.getHullId());
        if (data == null) return;
        for (int i = 1; i <= data.getMaxLevel(); i++) {
            levels.add(i);
        }
        generateMasteries(spec, levels, 0, false);

        Map<String, List<Set<Integer>>> rerollMap = (Map<String, List<Set<Integer>>>) Global.getSector().getPersistentData().get(REROLL_SEQUENCE_MAP);
        if (rerollMap == null) return;
        List<Set<Integer>> rerollSequence = rerollMap.get(spec.getHullId());
        if (rerollSequence == null) return;
        int seedPrefix = 1;
        for (Set<Integer> levelSet : rerollSequence) {
            generateMasteries(spec, levelSet, seedPrefix++, true);
        }
    }

    public static void generateMasteries(ShipHullSpecAPI spec, Set<Integer> levels, int seedPrefix, boolean avoidSeen) throws InstantiationException, IllegalAccessException, NoSuchMethodException {
        HullMasteryData data = masteryMap.get(spec.getHullId());
        Set<String> seenParams = new HashSet<>();
        Set<Class<?>> seenEffectClasses = new HashSet<>();
        for (int i : levels) {
            MasteryLevelData levelData = data.getDataForLevel(i);
            if (levelData != null) {
                for (var entry : levelData.getEffectsLists().entrySet()) {
                    for (MasteryEffect effect : entry.getValue()) {
                        seenEffectClasses.add(effect.getClass());
                        String[] args = effect.getArgs();
                        // First arg is strength, that shouldn't be avoided when avoiding repeats
                        seenParams.addAll(Arrays.asList(args).subList(1, args.length));
                    }
                }
                levelData.clear();
            }
        }

        for (int i : levels) {
            MasteryLevelData levelData = data.getDataForLevel(i);
            if (levelData != null) {
                levelData.generateEffects(
                        seedPrefix,
                        avoidSeen ? seenEffectClasses : new HashSet<>(),
                        avoidSeen ? seenParams : new HashSet<>());
            }
        }
    }

    public static void activatePlayerMasteries() {
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            ShipHullSpecAPI restoredSpec = Utils.getRestoredHullSpec(spec);
            if (spec != restoredSpec) continue;
            clearInvalidActiveLevels(spec);
            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSector().getPlayerPerson(), spec, effect -> effect.onActivate(Global.getSector().getPlayerPerson()));
        }
    }

    public static void clearRerolledMasteries() {
         for (String id : rerolledSpecs) {
             HullMasteryData data = masteryMap.get(id);
             if (data == null) continue;
             for (int i = 1; i <= data.getMaxLevel(); i++) {
                 MasteryLevelData levelData = data.getDataForLevel(i);
                 if (levelData == null) continue;
                 levelData.clear();
             }
             data.setGenerated(false);
         }
    }

    static void clearInvalidActiveLevels(ShipHullSpecAPI spec) {
        spec = Utils.getRestoredHullSpec(spec);
        SaveData data = SAVE_DATA_TABLE.get(spec.getHullId());
        if (data == null) return;
        Iterator<Map.Entry<Integer, String>> itr = data.activeLevels.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Integer, String> next = itr.next();
            if (next.getKey() > getMaxMasteryLevel(spec)) {
                itr.remove();
                continue;
            }
            // clear nonexistent mastery selections
            MasteryLevelData levelData = getLevelDataNoGenerate(spec, next.getKey());
            if (levelData == null || (levelData.getGenerators(next.getValue()).isEmpty())) {
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

    public static Float getCachedSelectionWeight(String masteryName, ShipHullSpecAPI spec) {
        spec = Utils.getRestoredHullSpec(spec);
        Map<String, Float> weights = selectionWeightMap.get(masteryName);
        if (weights == null) return null;
        return weights.get(spec.getHullId());
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
