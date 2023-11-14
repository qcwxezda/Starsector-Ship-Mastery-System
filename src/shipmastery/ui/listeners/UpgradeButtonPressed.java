package shipmastery.ui.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.Action;
import shipmastery.campaign.DeferredActionPlugin;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;

public class UpgradeButtonPressed extends ActionListener {
    MasteryPanel masteryPanel;
    String defaultText;
    ShipHullSpecAPI spec;

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
            MasteryUtils.spendMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
            MasteryUtils.advanceMasteryLevel(spec);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    Strings.UPGRADE_CONFIRMED_STR + MasteryUtils.getMasteryLevel(spec), Misc.getStoryBrightColor());
            Global.getSoundPlayer().playUISound("sms_increase_mastery", 1f, 1f);
            masteryPanel.forceRefresh(true);
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
