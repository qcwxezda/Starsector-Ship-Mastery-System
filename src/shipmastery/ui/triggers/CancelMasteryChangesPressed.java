package shipmastery.ui.triggers;

import shipmastery.ui.MasteryPanel;

public class CancelMasteryChangesPressed extends ActionListener {

    final MasteryPanel masteryPanel;

    public CancelMasteryChangesPressed(MasteryPanel masteryPanel) {
        this.masteryPanel = masteryPanel;
    }

    @Override
    public void trigger(Object... args) {
        masteryPanel.forceRefresh(false, false, true, false);
    }
}
