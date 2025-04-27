package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.Utils;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class ConfirmMasteryChangesPressed extends ActionListener {

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    public ConfirmMasteryChangesPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        Map<Integer, String> activeSet = ShipMastery.getPlayerActiveMasteriesCopy(spec);
        Map<Integer, String> newSet = masteryPanel.getSelectedMasteryButtons();
        NavigableMap<Integer, String> toActivate = new TreeMap<>();
        NavigableMap<Integer, String> toDeactivate = new TreeMap<>();

        for (Map.Entry<Integer, String> entry : activeSet.entrySet()) {
            int level = entry.getKey();
            String active = entry.getValue();
            if (!newSet.containsKey(level) || !Objects.equals(active, newSet.get(level))) {
                toDeactivate.put(level, active);
            }
        }

        for (Map.Entry<Integer, String> entry : newSet.entrySet()) {
            int level = entry.getKey();
            String newId = entry.getValue();
            if (!activeSet.containsKey(level) || !Objects.equals(activeSet.get(level), newId)) {
                toActivate.put(level, newId);
            }
        }

        for (Map.Entry<Integer, String> entry : toDeactivate.descendingMap().entrySet()) {
            ShipMastery.deactivatePlayerMastery(spec, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, String> entry : toActivate.entrySet()) {
            ShipMastery.activatePlayerMastery(spec, entry.getKey(), entry.getValue());
        }

        Global.getSoundPlayer().playUISound("sms_change_masteries", 1f, 1f);

        // This may make the player's fleet state invalid, i.e. if changing masteries removed a hangar
        // bay on ships that filled it
        Utils.fixPlayerFleetInconsistencies();

        masteryPanel.forceRefresh(true, false, true, false);
    }
}
