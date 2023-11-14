package shipmastery.ui.listeners;

import shipmastery.ui.MasteryPanel;

public class CancelMasteryChangesPressed extends ActionListener {

    MasteryPanel masteryPanel;

    public CancelMasteryChangesPressed(MasteryPanel masteryPanel) {
        this.masteryPanel = masteryPanel;
    }

    @Override
    public void trigger(Object... args) {
        masteryPanel.forceRefresh(false, true);
    }
}