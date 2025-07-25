package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.MasteryTags;
import shipmastery.plugin.ModPlugin;

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
    public static final float[] ENHANCE_MASTERY_AMOUNT = {0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f};
    public static final float[] ENHANCE_BONUS_XP = {0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.25f, 0.25f, 0.25f, 0.25f, 0.25f};
    public static final float ENHANCE_BONUS_XP_CIV = 0.9f;
    public static final int MAX_ENHANCES = 10;
    public static final int UNLOCK_SELECTIVE_RESTORATION_LEVEL;
    public static final int UNLOCK_SMOD_REMOVAL_LEVEL;
    public static final int UNLOCK_MASTERY_SHARING_LEVEL;
    public static final int UNLOCK_REROLL_LEVEL;
    public static final int UNLOCK_PSEUDOCORE_INTEGRATION_LEVEL;
    public static final String ENHANCE_MAP = "$sms_EnhanceMap";
    public static final String ENHANCE_MODIFIER_ID = "sms_enhancement";

    static {
        try {
            JSONObject presets = Global.getSettings().getMergedJSON("data/shipmastery/mastery_unlocks.json");
            UNLOCK_SELECTIVE_RESTORATION_LEVEL = presets.getInt("unlockSelectiveRestorationLevel");
            UNLOCK_SMOD_REMOVAL_LEVEL = presets.getInt("unlockSModRemovalLevel");
            UNLOCK_MASTERY_SHARING_LEVEL = presets.getInt("unlockMasterySharingLevel");
            UNLOCK_REROLL_LEVEL = presets.getInt("unlockRerollLevel");
            UNLOCK_PSEUDOCORE_INTEGRATION_LEVEL = presets.getInt("unlockPseudocoreIntegrationLevel");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static float getEnhanceBonusXP(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return ENHANCE_BONUS_XP_CIV;
        int count = Math.min(getEnhanceCount(spec), ENHANCE_BONUS_XP.length-1);
        return ENHANCE_BONUS_XP[count];
    }

    public static int getEnhanceMPCost(ShipHullSpecAPI spec) {
        int count = getEnhanceCount(spec);
        if (count >= MAX_ENHANCES) return Integer.MAX_VALUE;
        return switch (count) {
            case 0 -> 300;
            case 1 -> 450;
            case 2 -> 650;
            case 3 -> 900;
            case 4 -> 1200;
            case 5 -> 1550;
            case 6 -> 1950;
            case 7 -> 2400;
            case 8 -> 3000;
            case 9 -> 4000;
            default -> Integer.MAX_VALUE;
        };
    }

    public static int getEnhanceCount(ShipHullSpecAPI spec) {
        String baseId = Utils.getRestoredHullSpecId(spec);
        //noinspection unchecked
        Map<String, Integer> enhanceMap = (Map<String, Integer>) Global.getSector().getPersistentData().get(ENHANCE_MAP);
        if (enhanceMap == null) {
            return 0;
        }
        Integer enhanceCount = enhanceMap.get(baseId);
        return enhanceCount == null ? 0 : enhanceCount;
    }

    public static int getEnhanceSPCost() {
        return 1;
    }

    public static boolean hasEnoughXPToUpgradeOrEnhance(ShipHullSpecAPI spec) {
        var pts = ShipMastery.getPlayerMasteryPoints(spec);
        if (ShipMastery.getPlayerMasteryLevel(spec) < ShipMastery.getMaxMasteryLevel(spec)) {
            return pts >= getUpgradeCost(spec);
        }
        if (MasteryUtils.getEnhanceCount(spec) >= MasteryUtils.MAX_ENHANCES) return false;
        return pts >= getEnhanceMPCost(spec);
    }

    public static int getUpgradeCost(ShipHullSpecAPI spec) {
        return getUpgradeCost(ShipMastery.getPlayerMasteryLevel(spec));
    }

    public static int getUpgradeCost(int curLevel) {
        return switch (curLevel) {
            case 0 -> 60;
            case 1 -> 75;
            case 2 -> 100;
            case 3 -> 135;
            case 4 -> 180;
            case 5 -> 300;
            case 6 -> 500;
            default -> 500 + (curLevel-6)*100;
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

    public static float getModifiedMasteryEffectStrength(PersonAPI commander, ShipHullSpecAPI spec, float baseStrength) {
        spec = Utils.getRestoredHullSpec(spec);
        if (commander == null) return baseStrength;
        var stat1 = commander.getStats().getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD);
        var stat2 = commander.getStats().getDynamic().getMod(MasteryEffect.MASTERY_STRENGTH_MOD_FOR + spec.getHullId());
        var flat = stat1.getFlatBonus() + stat2.getFlatBonus();
        var percent = stat1.getPercentMod() + stat2.getPercentMod();
        var mult = stat1.getMult() * stat2.getMult();
        return (baseStrength + flat + baseStrength * percent / 100f) * mult;
    }

    public static int getPlayerUnassignedCount(ShipHullSpecAPI spec) {
        int maxLevel = ShipMastery.getPlayerMasteryLevel(spec);
        Set<Integer> assignedLevels = ShipMastery.getPlayerActiveMasteriesCopy(spec).keySet();
        int unassignedLevels = 0;
        for (int i = 1; i <= maxLevel; i++) {
            if (!assignedLevels.contains(i)) {
                unassignedLevels++;
            }
        }
        return unassignedLevels;
    }

    public interface MasteryAction {
        void perform(MasteryEffect effect);
    }
}
