package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.mastery.MasteryEffect;
import shipmastery.mastery.impl.compound.CompoundMastery;
import java.util.SortedSet;
import java.util.TreeSet;

public abstract class MasteryUtils {
    public static int getMaxMastery(ShipHullSpecAPI spec) {
        String id = Utils.getBaseHullId(spec);
        return ShipMastery.masteryMap.get(id).size();
    }

    public static int getUpgradeCost(ShipHullSpecAPI spec) {
        int level = getMasteryLevel(spec);
        return (2 + level) * 2;
    }

    public static int getMasteryLevel(ShipHullSpecAPI spec) {
        if (ShipMastery.MASTERY_TABLE == null) return 0;

        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(Utils.getBaseHullId(spec));
        return data == null ? 0 : data.level;
    }

    public static void advanceMasteryLevel(ShipHullSpecAPI spec) {
        String id = Utils.getBaseHullId(spec);
        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(id);

        if (data == null) {
            data = new ShipMastery.MasteryData(0, 1);
            ShipMastery.MASTERY_TABLE.put(id, data);
        } else {
            data.level++;
        }

        MasteryEffect effect = getMasteryEffect(spec, data.level);
        if (effect.isAutoActivateWhenUnlocked(spec)) {
            activateMastery(spec, data.level);
        }
    }

    public static float getMasteryPoints(ShipHullSpecAPI spec) {
        if (ShipMastery.MASTERY_TABLE == null) return 0f;

        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(Utils.getBaseHullId(spec));
        return data == null ? 0 : data.points;
    }

    public static void addMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getBaseHullId(spec);
        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(id);
        if (data == null) {
            ShipMastery.MASTERY_TABLE.put(id, new ShipMastery.MasteryData(amount, 0));
        }
        else {
            data.points += amount;
        }
    }

    public static void spendMasteryPoints(ShipHullSpecAPI spec, float amount) {
        String id = Utils.getBaseHullId(spec);
        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(id);
        if (data == null) return;

        data.points -= amount;
        data.points = Math.max(0f, data.points);
    }

    public static void activateMastery(ShipHullSpecAPI spec, int level) {
        String id = Utils.getBaseHullId(spec);
        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(id);

        if (data == null) {
            data = new ShipMastery.MasteryData(0, 0);
            ShipMastery.MASTERY_TABLE.put(id, data);
        }
        data.activeLevels.add(level);
    }

    public static void deactivateMastery(ShipHullSpecAPI spec, int level) {
        String id = Utils.getBaseHullId(spec);
        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(id);

        if (data == null) {
            data = new ShipMastery.MasteryData(0, 0);
            ShipMastery.MASTERY_TABLE.put(id, data);
        }

        data.activeLevels.remove(level);
    }

    public static SortedSet<Integer> getActiveMasteries(ShipHullSpecAPI spec) {
        if (ShipMastery.MASTERY_TABLE == null) return new TreeSet<>();

        ShipMastery.MasteryData data = ShipMastery.MASTERY_TABLE.get(Utils.getBaseHullId(spec));
        return data == null ? new TreeSet<Integer>() : data.activeLevels;
    }

    public static MasteryEffect getMasteryEffect(ShipHullSpecAPI spec, int level) {
        String id = Utils.getBaseHullId(spec);
        return ShipMastery.masteryMap.get(id).get(level - 1);
    }

    public static MasteryEffect getMasteryEffect(String specId, int level) {
        return getMasteryEffect(Global.getSettings().getHullSpec(specId), level);
    }

    public static String makeEffectId(MasteryEffect effect, int level) {
        String id = makeSharedId(effect);
        if (!effect.isUniqueEffect()) {
            id += "_" + level;
        }
        return id;
    }

    public static String makeSharedId(MasteryEffect effect) {
        return "shipmastery_" + ShipMastery.getId(effect.getClass());
    }
}
