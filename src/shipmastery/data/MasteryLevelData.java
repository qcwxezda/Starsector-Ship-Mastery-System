package shipmastery.data;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.MasteryEffect;

import java.util.ArrayList;
import java.util.List;

/** Data for a specific mastery level. One instance per ship hull type per level. */
public class MasteryLevelData {

    final List<MasteryEffect> effectsListOption1 = new ArrayList<>();

    final List<MasteryEffect> effectsListOption2 = new ArrayList<>();
    final ShipHullSpecAPI spec;
    final int level;

    public MasteryLevelData(ShipHullSpecAPI spec, int level) {
        this.spec = spec;
        this.level = level;
    }

    public void addEffectToOption1(MasteryEffect effect) {
        effectsListOption1.add(effect);
    }

    public void addEffectToOption2(MasteryEffect effect) {
        effectsListOption2.add(effect);
    }

    public List<MasteryEffect> getEffectsListOption1() {
        return effectsListOption1;
    }

    public List<MasteryEffect> getEffectsListOption2() {
        return effectsListOption2;
    }
}
