package shipmastery.util;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;

import java.util.*;

public abstract class MasteryUtils {

    public static int getUpgradeCost(ShipHullSpecAPI spec) {
        int level = ShipMastery.getMasteryLevel(spec);
        return (2 + level) * 2;
    }

    public static String makeEffectId(MasteryEffect effect, int level, int index) {
        String id = makeSharedId(effect);
        if (!isUnique(effect)) {
            id += "_" + level + "_" + index;
        }
        return id;
    }

    public static String makeSharedId(MasteryEffect effect) {
        return "shipmastery_" + ShipMastery.getId(effect.getClass());
    }

    /**
     * If the set of masteries to be applied contains duplicate unique masteries, only one is applied.
     * Masteries are applied in level order, and sequentially within each mastery level.
     * Note: the unique mastery used is the one with the highest mastery level (and, if part of the same level, the highest index
     *       in that level). This is to ensure that between activate and deactivate or beginRefit and endRefit calls,
     *       the selected unique mastery doesn't change. For example, if we were to pick the strongest unique effect,
     *       and another effect increased the strength of a different effect so that it is now stronger than the previously strongest
     *       effect, we could have a scenario in which endRefit doesn't exactly undo beginRefit.
     * */
    public static void applyAllMasteryEffects(ShipHullSpecAPI spec, Set<Integer> levelsToApply, boolean reverseOrder, MasteryAction action) {
        // Effect id -> the actual effect to be executed
        Map<Class<?>, MasteryEffect> uniqueEffects = new HashMap<>();

        // Sort the levels set so that lower mastery levels are applied first
        NavigableSet<Integer> sortedLevels = new TreeSet<>(levelsToApply);

        for (int i : sortedLevels) {
            for (MasteryEffect effect : ShipMastery.getMasteryEffects(spec, i)) {
                if (isUnique(effect)) {
                    uniqueEffects.put(effect.getClass(), effect);
                }
            }
        }

        for (int i : reverseOrder ? sortedLevels.descendingSet() : sortedLevels) {
            List<MasteryEffect> effects = ShipMastery.getMasteryEffects(spec, i);
            for (int j = 0, k = effects.size() - 1; j < effects.size(); j++, k--) {
                int index = reverseOrder ? k : j;
                MasteryEffect effect = effects.get(index);
                if (!isUnique(effect)  || effect.equals(uniqueEffects.get(effect.getClass()))) {
                    action.perform(effect, makeEffectId(effect, i, index));
                }
            }
        }
    }

    public static boolean isUnique(MasteryEffect effect) {
        return ShipMastery.hasTag(effect, MasteryTags.TAG_UNIQUE);
    }

    public static boolean hasTooltip(MasteryEffect effect) {
        return ShipMastery.hasTag(effect, MasteryTags.TAG_HAS_TOOLTIP);
    }

    public static boolean canDisable(MasteryEffect effect) {
        return !ShipMastery.hasTag(effect, MasteryTags.TAG_NO_DISABLE);
    }

    public static boolean isAutoActivate(MasteryEffect effect) {
        return !ShipMastery.hasTag(effect, MasteryTags.TAG_NO_AUTO_ACTIVATE);
    }

    public static boolean alwaysShowDescription(MasteryEffect effect) {
        return ShipMastery.hasTag(effect, MasteryTags.TAG_NO_HIDE_DESCRIPTION);
    }

    public static void applyAllActiveMasteryEffects(ShipHullSpecAPI spec, MasteryAction action) {
        applyAllMasteryEffects(spec, ShipMastery.getActiveMasteries(spec), false, action);
    }

    public interface MasteryAction {
        void perform(MasteryEffect effect, String id);
    }
}
