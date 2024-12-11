package shipmastery.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.MasteryEffect;

import java.util.ArrayList;
import java.util.List;

/** Data for a specific mastery level. One instance per ship hull type per level. */
public class MasteryLevelData {
    /** Reloaded when save file changes */
    final List<MasteryEffect> effectsListOption1 = new ArrayList<>();

    final List<MasteryEffect> effectsListOption2 = new ArrayList<>();

    /** Created once on game load */
    final List<MasteryGenerator> generatorsOption1 = new ArrayList<>();
    final List<MasteryGenerator> generatorsOption2 = new ArrayList<>();
    final String hullOrPresetName;
    final ShipHullSpecAPI spec;
    final int level;

    public void generateEffects(int seedPrefix) throws InstantiationException, IllegalAccessException {
        if (spec == null) {
            throw new RuntimeException(hullOrPresetName + " is a preset; can't generate masteries for a preset");
        }
        for (int i = 0; i < generatorsOption1.size(); i++) {
            effectsListOption1.add(generatorsOption1.get(i).generate(spec, level, i, false, seedPrefix));
        }
        for (int i = 0; i < generatorsOption2.size(); i++) {
            effectsListOption2.add(generatorsOption2.get(i).generate(spec, level, i, true, seedPrefix));
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

    public void addGeneratorToOption1(MasteryGenerator generator) {
        generatorsOption1.add(generator);
    }

    public void addGeneratorToOption2(MasteryGenerator generator) {
        generatorsOption2.add(generator);
    }

    public List<MasteryGenerator> getGeneratorsOption1() {
        return generatorsOption1;
    }

    public List<MasteryGenerator> getGeneratorsOption2() {
        return generatorsOption2;
    }

    public List<MasteryEffect> getEffectsListOption1() {
        return effectsListOption1;
    }

    public List<MasteryEffect> getEffectsListOption2() {
        return effectsListOption2;
    }

    public void clear() {
        effectsListOption1.clear();
        effectsListOption2.clear();
    }
}
