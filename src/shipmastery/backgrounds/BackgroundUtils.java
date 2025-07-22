package shipmastery.backgrounds;

import com.fs.starfarer.api.Global;

public abstract class BackgroundUtils {
    // Compile-time constant, doesn't require Enlightened (Nex dependency) to be loaded
    public static boolean isEnlightenedStart() {
        return (boolean) Global.getSector().getPersistentData().getOrDefault(Enlightened.IS_ENLIGHTENED_START, false);
    }

    // Compile-time constant, doesn't require HullTinkerer (Nex dependency) to be loaded
    public static boolean isTinkererStart() {
        return (boolean) Global.getSector().getPersistentData().getOrDefault(HullTinkerer.IS_TINKERER_START, false);
    }

    // Compile-time constant, doesn't require RejectHumanity (Nex dependency) to be loaded
    public static boolean isRejectHumanityStart() {
        return (boolean) Global.getSector().getPersistentData().getOrDefault(RejectHumanity.IS_REJECT_HUMANITY_START, false);
    }
}
