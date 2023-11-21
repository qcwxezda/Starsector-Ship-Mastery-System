package shipmastery.data;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ui.CustomUIElement;

import java.util.ArrayList;
import java.util.List;

/** Mastery data. One instance per ship hull type. */
public class HullMasteryData implements CustomUIElement {
    final List<MasteryLevelData> levels = new ArrayList<>();
    final ShipHullSpecAPI spec;

    public HullMasteryData(ShipHullSpecAPI spec) {
        this.spec = spec;
    }

    public MasteryLevelData getDataForLevel(int level) {
        return levels.get(level);
    }

    public int getMaxLevel() {
        return levels.size();
    }

    public void addLevelData(MasteryLevelData levelData) {
        levels.add(levelData);
    }

    public void setLevelData(MasteryLevelData levelData, int index) {
        assert (index < levels.size());
        levels.set(index, levelData);
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {

    }
}
