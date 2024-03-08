package shipmastery.ui.triggers;

import shipmastery.ui.MasteryPanel;

public class SModTableHeaderPressed extends ActionListener {

    final MasteryPanel masteryPanel;
    final int columnIndex;

    public SModTableHeaderPressed(MasteryPanel masteryPanel, int columnIndex) {
        this.masteryPanel = masteryPanel;
        this.columnIndex = columnIndex;
    }

    @Override
    public void trigger(Object... args) {
        masteryPanel.setComparatorAndRefresh(MasteryPanel.columnNames[columnIndex]);
    }
}
