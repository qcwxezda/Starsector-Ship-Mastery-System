package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.jetbrains.annotations.NotNull;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.plugin.ModPlugin;
import shipmastery.ui.EnhanceMasteryDisplay;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

public abstract class MasteryUtils {

    public static final int bonusLogisticSlotEnhanceNumber = 9999; // Disabled, sorry
    public static final String CONSTRUCT_MP_OVERRIDE_KEY = "$sms_ConstructMPOverride";
    public static final float[] ENHANCE_MASTERY_AMOUNT = {0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.02f, 0.02f, 0.02f, 0.02f, 0.02f};
    public static final float[] ENHANCE_DR_AMOUNT = {0f, 0f, 0f, 0f, 0f, 0.01f, 0.01f, 0.01f, 0.01f, 0.01f};
    public static final float[] ENHANCE_BONUS_XP = {0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f};
    public static final int MAX_ENHANCES = 10;

    public static int getRerollMPCost(@SuppressWarnings("unused") ShipHullSpecAPI spec) {
        //noinspection unchecked
        Map<String, List<Set<Integer>>> rerollMap = (Map<String, List<Set<Integer>>>) Global.getSector().getPersistentData().get(ShipMastery.REROLL_SEQUENCE_MAP);
        if (rerollMap == null) return 25;
        return 25 + Settings.ADDITIONAL_MP_PER_REROLL*rerollMap.getOrDefault(Utils.getRestoredHullSpecId(spec), Collections.emptyList()).size();
    }

    public static int getRerollSPCost(@SuppressWarnings("unused") ShipHullSpecAPI spec) {
        return 1;
    }

    public static int getConstructCost() {
        return (int) Global.getSector().getPersistentData().getOrDefault(CONSTRUCT_MP_OVERRIDE_KEY, KnowledgeConstructPlugin.NUM_POINTS_GAINED);
    }

    public static int getEnhanceMPCost(ShipHullSpecAPI spec) {
        int count = getEnhanceCount(spec);
        if (count >= MAX_ENHANCES) return Integer.MAX_VALUE;
        return switch (count) {
            case 0 -> 25;
            case 1 -> 27;
            case 2 -> 30;
            case 3 -> 34;
            case 4 -> 38;
            case 5 -> 42;
            case 6 -> 47;
            case 7 -> 52;
            case 8 -> 58;
            case 9 -> 99;
            default -> Integer.MAX_VALUE;
        };
    }

    public static int getEnhanceCount(ShipHullSpecAPI spec) {
        String baseId = Utils.getRestoredHullSpecId(spec);
        //noinspection unchecked
        Map<String, Integer> enhanceMap = (Map<String, Integer>) Global.getSector().getPersistentData().get(EnhanceMasteryDisplay.ENHANCE_MAP);
        if (enhanceMap == null) {
            return 0;
        }
        Integer enhanceCount = enhanceMap.get(baseId);
        return enhanceCount == null ? 0 : enhanceCount;
    }

    public static int getEnhanceSPCost(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return 0;
        return 1;
    }

    public static int getUpgradeCost(ShipHullSpecAPI spec) {
        int level = ShipMastery.getPlayerMasteryLevel(spec);
        return switch (level) {
            case 0 -> 3;
            case 1 -> 4;
            case 2 -> 6;
            case 3 -> 8;
            case 4 -> 10;
            case 5 -> 13;
            case 6 -> 16;
            case 7 -> 20;
            case 8 -> 25;
            default -> 25 + (level-8)*4;
        };
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
     * Masteries are applied in priority order, then level order, and finally, sequentially within each mastery level.
     * Note: the unique mastery used is the one with the highest mastery level (and, if part of the same level, the highest index
     *       in that level). This is to ensure that between activate and deactivate or beginRefit and endRefit calls,
     *       the selected unique mastery doesn't change. For example, if we were to pick the strongest unique effect,
     *       and another effect increased the strength of a different effect so that it is now stronger than the previously strongest
     *       effect, we could have a scenario in which endRefit doesn't exactly undo beginRefit.
     * */
    public static void applyMasteryEffects(ShipHullSpecAPI spec, Map<Integer, String> levelsToApply, boolean reverseOrder, MasteryAction action) {
        if (levelsToApply.isEmpty()) return;
        // Effect id -> the actual effect to be executed
        Map<Class<?>, MasteryEffect> uniqueEffects = new HashMap<>();
        // Sort the levels set so that lower mastery levels are applied first
        Map<Integer, String> sortedLevels = new TreeMap<>(levelsToApply);
        PriorityQueue<MasteryEffectData> priorityOrder = new PriorityQueue<>(10, reverseOrder ? Collections.reverseOrder() : null);

        for (Map.Entry<Integer, String> levelData : sortedLevels.entrySet()) {
            List<MasteryEffect> masteryEffects = ShipMastery.getMasteryEffects(spec, levelData.getKey(), levelData.getValue());
            for (int j = 0; j < masteryEffects.size(); j++) {
                MasteryEffect effect = masteryEffects.get(j);
                if (isUnique(effect)) {
                    uniqueEffects.put(effect.getClass(), effect);
                }
                priorityOrder.add(new MasteryEffectData(effect, levelData.getKey(), j));
            }
        }

        //System.out.println("Performing: ");
        while (!priorityOrder.isEmpty()) {
            MasteryEffectData data = priorityOrder.poll();
            MasteryEffect effect = data.effect;
            if (!isUnique(effect)  || effect.equals(uniqueEffects.get(effect.getClass()))) {
                action.perform(effect);
                //System.out.print("(" + data.level + ", " + data.index + ")  ");
            }
        }
        //System.out.println();
    }

    public static String getRandomMasterySeed() {
        String seed = (String) Global.getSector().getPersistentData().get(ModPlugin.GENERATION_SEED_KEY);
        return seed == null ? Global.getSector().getPlayerPerson().getId() : seed;
    }

    private record MasteryEffectData(MasteryEffect effect, int level,
                                     int index) implements Comparable<MasteryEffectData> {

        @Override
            public int compareTo(@NotNull MasteryUtils.MasteryEffectData o) {
                int p = Integer.compare(effect.getPriority(), o.effect.getPriority());
                if (p != 0) return p;
                int q = Integer.compare(level, o.level);
                if (q != 0) return q;
                return Integer.compare(index, o.index);
            }
        }

    public static boolean isUnique(MasteryEffect effect) {
        return effect.hasTag(MasteryTags.UNIQUE);
    }

    public static boolean hasTooltip(MasteryEffect effect) {
        return effect.hasTag(MasteryTags.HAS_TOOLTIP);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canDisable(MasteryEffect effect) {
        return !effect.hasTag(MasteryTags.NO_DISABLE);
    }

    public static boolean isAutoActivate(MasteryEffect effect) {
        // Don't auto-activate triggers-autofit effects due to weird edge-case behaviors
        // e.g. if an effect reduces the number of fighter bays a ship has, then vanilla (added in 0.98) will add excess wings to cargo
        // but if this can be undone, the excess wings can be duplicated!
        return !effect.hasTag(MasteryTags.NO_AUTO_ACTIVATE) && !effect.hasTag(MasteryTags.TRIGGERS_AUTOFIT);
    }

    public static boolean alwaysShowDescription(MasteryEffect effect) {
        return effect.hasTag(MasteryTags.NO_HIDE_DESCRIPTION);
    }

    public static void applyAllActiveMasteryEffects(PersonAPI commander, ShipHullSpecAPI spec, MasteryAction action) {
        if (commander == null) return;
        Map<Integer, String> levelsToApply = FleetHandler.getActiveMasteriesForCommander(commander, spec);
        applyMasteryEffects(spec, levelsToApply, false, action);
    }

    public interface MasteryAction {
        void perform(MasteryEffect effect);
    }
}
