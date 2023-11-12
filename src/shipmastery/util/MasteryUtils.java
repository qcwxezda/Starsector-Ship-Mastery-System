package shipmastery.util;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;

public abstract class MasteryUtils {
    public static int getMaxMastery(ShipHullSpecAPI spec) {
        return 12;
    }

    public static String getMasteryDescription(ShipHullSpecAPI spec, int level) {
        return "This is some test text.\nThis is another line of test text.\n This is yet another line of test text. This line is particularly long and goes on and on and on and so on ";
    }
}
