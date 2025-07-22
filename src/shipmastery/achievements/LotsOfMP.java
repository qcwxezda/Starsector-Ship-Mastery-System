package shipmastery.achievements;

import org.jetbrains.annotations.Nullable;
import org.magiclib.achievements.MagicAchievement;

public class LotsOfMP extends MagicAchievement {
    public static final int NUM_NEEDED = 9000;

    @Override
    public boolean getHasProgressBar() {
        return true;
    }

    @Override
    public @Nullable Float getMaxProgress() {
        return (float) NUM_NEEDED;
    }
}
