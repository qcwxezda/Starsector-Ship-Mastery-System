package shipmastery.listeners;

import shipmastery.ui.MasteryPanel;

public class SModTableHeaderPressed extends ActionListener {

    MasteryPanel masteryPanel;
    int columnIndex;

    public SModTableHeaderPressed(MasteryPanel masteryPanel, int columnIndex) {
        this.masteryPanel = masteryPanel;
        this.columnIndex = columnIndex;
    }

    @Override
    public void trigger(Object... args) {
        masteryPanel.setComparatorAndRefresh(MasteryPanel.columnNames[columnIndex]);
    }
}
