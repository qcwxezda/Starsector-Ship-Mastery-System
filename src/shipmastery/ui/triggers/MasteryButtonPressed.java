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
        ShipAPI root = handler.getSelectedShip().two;
        if (root == null) return;
        ShipHullSpecAPI spec = Utils.getRestoredHullSpec(root.getHullSpec());
        try {
            if (!ShipMastery.hasMasteryData(spec)) {
                ShipMastery.generateMasteries(spec);
            }
            new MasteryPanel(handler);
        } catch (InstantiationException | IllegalAccessException e) {
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    Strings.FAILED_TO_GENERATE_MASTERIES,
                    Misc.getNegativeHighlightColor());
            e.printStackTrace();
        }
    }
}
