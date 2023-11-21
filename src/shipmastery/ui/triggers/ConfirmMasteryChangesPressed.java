package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.ui.MasteryPanel;

import java.util.*;

public class ConfirmMasteryChangesPressed extends ActionListener {

    MasteryPanel masteryPanel;
    ShipHullSpecAPI spec;

    public ConfirmMasteryChangesPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        Map<Integer, Boolean> activeSet = ShipMastery.getActiveMasteriesCopy(spec);
        Map<Integer, Boolean> newSet = masteryPanel.getSelectedMasteryButtons();
        NavigableMap<Integer, Boolean> toActivate = new TreeMap<>();
        NavigableMap<Integer, Boolean> toDeactivate = new TreeMap<>();

        for (Map.Entry<Integer, Boolean> entry : activeSet.entrySet()) {
            int level = entry.getKey();
            boolean wasOption2 = entry.getValue();
            if (!newSet.containsKey(level) || newSet.get(level) != wasOption2) {
                toDeactivate.put(level, wasOption2);
            }
        }

        for (Map.Entry<Integer, Boolean> entry : newSet.entrySet()) {
            int level = entry.getKey();
            boolean isOption2 = entry.getValue();
            if (!activeSet.containsKey(level) || activeSet.get(level) != isOption2) {
                toActivate.put(level, isOption2);
            }
        }

        for (Map.Entry<Integer, Boolean> entry : toDeactivate.descendingMap().entrySet()) {
            ShipMastery.deactivateMastery(spec, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, Boolean> entry : toActivate.entrySet()) {
            ShipMastery.activateMastery(spec, entry.getKey(), entry.getValue());
        }

        Global.getSoundPlayer().playUISound("sms_change_masteries", 1f, 1f);
        masteryPanel.forceRefresh(true, true);
    }
}
