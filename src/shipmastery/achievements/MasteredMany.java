package shipmastery.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import org.jetbrains.annotations.Nullable;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.ShipMastery;
import shipmastery.util.Utils;

import java.util.HashSet;
import java.util.Set;

public class MasteredMany extends MagicAchievement {
    public static final int NUM_NEEDED = 24;
    public static final String MASTERED_COUNT_KEY = "$sms_MasteredCountKey";

    public static void refreshPlayerMasteredCount() {
        Set<String> countedBaseIds = new HashSet<>();
        int count = 0;
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            ShipHullSpecAPI restoredSpec = Utils.getRestoredHullSpec(spec);
            if (spec != restoredSpec) continue;
            String baseId = spec.getBaseHullId();
            if (countedBaseIds.contains(baseId)) continue;

            if (ShipMastery.getPlayerMasteryLevel(spec) >= ShipMastery.getMaxMasteryLevel(spec)) {
                count++;
                countedBaseIds.add(baseId);
            }
        }
        Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().set(MASTERED_COUNT_KEY, count);
    }

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
        return (float) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().getInt(MASTERED_COUNT_KEY);
    }
}
