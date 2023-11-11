package shipmastery.listeners;

import com.fs.starfarer.api.ui.ButtonAPI;

public class TabButtonPressed extends ActionListener {

    MasteryButtonPressed parentListener;

    public TabButtonPressed(MasteryButtonPressed parentListener) {
        this.parentListener = parentListener;
    }

    @Override
    public void trigger(Object... args) {
        ButtonAPI button = (ButtonAPI) args[1];
        button.setChecked(true);
        parentListener.togglePanelVisibility(button);
    }
}
