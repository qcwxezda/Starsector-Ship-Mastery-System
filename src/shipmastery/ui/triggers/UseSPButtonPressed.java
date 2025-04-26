package shipmastery.ui.triggers;

import shipmastery.ui.MasteryPanel;

public class UseSPButtonPressed extends ActionListener {

    private final MasteryPanel panel;

    public UseSPButtonPressed(MasteryPanel masteryPanel) {
        panel = masteryPanel;
    }

    @Override
    public void trigger(Object... args) {
        panel.setUsingSP(!panel.isUsingSP());
        panel.forceRefresh(false, false, true, false);
    }
}
