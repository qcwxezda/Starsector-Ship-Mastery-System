package shipmastery.ui.triggers;

import com.fs.starfarer.api.ui.ButtonAPI;
import shipmastery.ui.MasteryPanel;

public class TabButtonPressed extends ActionListener {

    final MasteryPanel masteryPanel;

    public TabButtonPressed(MasteryPanel masteryPanel) {
        this.masteryPanel = masteryPanel;
    }

    @Override
    public void trigger(Object... args) {
        ButtonAPI button = (ButtonAPI) args[1];
        button.setChecked(true);
        masteryPanel.togglePanelVisibility(button);
    }
}
