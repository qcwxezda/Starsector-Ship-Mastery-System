package shipmastery.ui.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;

public class ConfirmMasteryChangesPressed extends ActionListener {

    MasteryPanel masteryPanel;

    public ConfirmMasteryChangesPressed(MasteryPanel masteryPanel) {
        this.masteryPanel = masteryPanel;
    }

    @Override
    public void trigger(Object... args) {

        ShipHullSpecAPI spec = masteryPanel.getShip().getHullSpec();

        MasteryUtils.getActiveMasteries(spec).clear();
        MasteryUtils.getActiveMasteries(spec).addAll(masteryPanel.getSelectedMasteryButtons());

        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.MASTERY_CHANGES_CONFIRMED,
                                                                          Misc.getHighlightColor());
        Global.getSoundPlayer().playUISound("sms_change_masteries", 1f, 1f);

        masteryPanel.forceRefresh(true, true);
    }
}
