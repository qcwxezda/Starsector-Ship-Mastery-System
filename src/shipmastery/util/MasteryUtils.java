package shipmastery.util;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.Settings;

public abstract class MasteryUtils {
    public static int getMaxMastery(ShipHullSpecAPI spec) {
        return 12;
    }

    public static int getUpgradeCost(ShipHullSpecAPI spec) {
        int level = Settings.getMasteryLevel(spec);
        return (2 + level) * 2;
    }
}
