package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class UpgradeButtonPressed extends ActionListener {
    final MasteryPanel masteryPanel;
    final String defaultText;
    final ShipHullSpecAPI spec;

    public UpgradeButtonPressed(MasteryPanel masteryPanel, String defaultText, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.defaultText = defaultText;
        this.spec = spec;
    }
    @Override
    public void trigger(Object... args) {
        final ButtonAPI button = (ButtonAPI) args[1];
        if (isConfirming(button)) {
            endConfirm(button);
            ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
            ShipMastery.advancePlayerMasteryLevel(spec);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    Strings.UPGRADE_CONFIRMED_STR + ShipMastery.getPlayerMasteryLevel(spec), Settings.MASTERY_COLOR);
            Global.getSoundPlayer().playUISound("sms_increase_mastery", 1f, 1f);
            masteryPanel.forceRefresh(true, false, false);

            Utils.fixPlayerFleetInconsistencies();
        }
        else {
            beginConfirm(button);
            DeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    endConfirm(button);
                }
            }, Settings.DOUBLE_CLICK_INTERVAL);
        }
    }

    void beginConfirm(ButtonAPI button) {
        button.setCustomData(true);
        button.setText(Strings.UPGRADE_ASK_STR);
    }

    void endConfirm(ButtonAPI button) {
        button.setCustomData(false);
        button.setText(defaultText);
    }

    boolean isConfirming(ButtonAPI button) {
        return button.getCustomData() != null && (boolean) button.getCustomData();
    }
}
