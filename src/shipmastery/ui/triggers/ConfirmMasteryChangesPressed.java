package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.ui.MasteryPanel;

import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public class ConfirmMasteryChangesPressed extends ActionListener {

    MasteryPanel masteryPanel;
    ShipHullSpecAPI spec;

    public ConfirmMasteryChangesPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        Set<Integer> union = new HashSet<>();
        Set<Integer> activeSet = ShipMastery.getActiveMasteriesCopy(spec);
        Set<Integer> newSet = masteryPanel.getSelectedMasteryButtons();
        NavigableSet<Integer> toActivate = new TreeSet<>();
        NavigableSet<Integer> toDeactivate = new TreeSet<>();
        union.addAll(activeSet);
        union.addAll(newSet);

        for (int i : union) {
            boolean wasActive = activeSet.contains(i);
            boolean nowActive = newSet.contains(i);

            if (wasActive && !nowActive) {
                // Effect was deactivated
                toDeactivate.add(i);
            }
            else if (!wasActive && nowActive) {
                // Effect was activated
                toActivate.add(i);
            }
        }

        for (int i : toDeactivate.descendingSet()) {
            ShipMastery.deactivateMastery(spec, i);
        }
        for (int i : toActivate) {
            ShipMastery.activateMastery(spec, i);
        }

        Global.getSoundPlayer().playUISound("sms_change_masteries", 1f, 1f);
        masteryPanel.forceRefresh(true, true);
    }
}
