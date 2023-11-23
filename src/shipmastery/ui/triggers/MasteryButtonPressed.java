package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.RefitHandler;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

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
