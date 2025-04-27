package shipmastery.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.MasteryEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/** Data for a specific mastery level. One instance per ship hull type per level. */
public class MasteryLevelData {
    /** Reloaded when save file changes */
    final SortedMap<String, List<MasteryEffect>> effectsLists = new TreeMap<>();
    /** Created once on game load */
    final SortedMap<String, List<MasteryGenerator>> generatorsLists = new TreeMap<>();

    final String hullOrPresetName;
    final ShipHullSpecAPI spec;
    final int level;

    public void generateEffects(int seedPrefix, Set<Class<?>> avoidWhenGenerating, Set<String> paramsToAvoidWhenGenerating) throws InstantiationException, IllegalAccessException, NoSuchMethodException {
        if (spec == null) {
            throw new RuntimeException(hullOrPresetName + " is a preset; can't generate masteries for a preset");
        }
        for (var entry : generatorsLists.entrySet()) {
            String key = entry.getKey();
            List<MasteryGenerator> generators = entry.getValue();
            List<MasteryEffect> effects = new ArrayList<>();
            for (int i = 0; i < generators.size(); i++) {
                effects.add(generators.get(i).generate(spec, level, i, key, seedPrefix, avoidWhenGenerating, paramsToAvoidWhenGenerating));
            }
            effectsLists.put(key, effects);
        }
    }

    public MasteryLevelData(String name, int level) {
        ShipHullSpecAPI spec1;
        hullOrPresetName = name;
        try {
            spec1 = Global.getSettings().getHullSpec(hullOrPresetName);
        }
        catch (Exception e) {
            spec1 = null;
        }
        spec = spec1;
        this.level = level;
    }

    public void addGeneratorToList(String id, MasteryGenerator generator) {
        generatorsLists.computeIfAbsent(id, k -> new ArrayList<>()).add(generator);
    }

    public List<MasteryGenerator> getGenerators(String id) {
        return generatorsLists.getOrDefault(id, new ArrayList<>());
    }

    public SortedMap<String, List<MasteryGenerator>> getGeneratorsLists() {
        return generatorsLists;
    }

    public SortedMap<String, List<MasteryEffect>> getEffectsLists() {
        return effectsLists;
    }

    public void clear() {
        effectsLists.clear();
    }
}
