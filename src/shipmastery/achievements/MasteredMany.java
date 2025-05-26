package shipmastery.achievements;

import com.fs.starfarer.api.Global;
import org.jetbrains.annotations.Nullable;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.campaign.skills.CyberneticAugmentation;

public class MasteredMany extends MagicAchievement {
    public static final int NUM_NEEDED = 20;

    @Override
    public boolean getHasProgressBar() {
        return true;
    }

    @Override
    public @Nullable Float getMaxProgress() {
        return (float) NUM_NEEDED;
    }

    @Override
    public @Nullable Float getProgress() {
        Integer num = (Integer) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get(CyberneticAugmentation.MASTERED_COUNT_KEY);
        return num == null ? 0f : num.floatValue();
    }
}
