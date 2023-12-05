package shipmastery.ui.triggers;

import shipmastery.campaign.RefitHandler;
import shipmastery.ui.MasteryPanel;

public class MasteryButtonPressed extends ActionListener {

    RefitHandler handler;
    public MasteryButtonPressed(RefitHandler handler) {
        this.handler = handler;
    }
    @Override
    public void trigger(Object... args) {
        new MasteryPanel(handler);
    }
}
