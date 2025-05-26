package shipmastery.achievements;

import com.fs.starfarer.api.Global;
import org.jetbrains.annotations.Nullable;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.procgen.Generator;
import shipmastery.util.Strings;

import java.util.Collection;

public class SalvagedAllStations extends MagicAchievement {
    @Override
    public boolean getHasProgressBar() {
        return true;
    }

    @Override
    public @Nullable Float getProgress() {
        Collection<?> ids = (Collection<?>) Global.getSector().getMemoryWithoutUpdate().get(Strings.Campaign.SALVAGED_BEACON_IDS);
        return ids == null ? 0f : ids.size();
    }

    @Override
    public @Nullable Float getMaxProgress() {
        return (float) (Generator.NUM_STATIONS_HULLMOD + Generator.NUM_STATIONS_ITEM);
    }
}
