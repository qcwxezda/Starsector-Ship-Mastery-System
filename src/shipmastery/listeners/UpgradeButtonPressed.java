package shipmastery.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.Settings;
import shipmastery.campaign.Action;
import shipmastery.campaign.DeferredActionPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Utils;

public class UpgradeButtonPressed extends ActionListener {
    static final String UPGRADE_CONFIRMED_STR = Utils.getString("sms_masteryPanel", "upgradeConfirm");
    static final String UPGRADE_ASK_STR = Utils.getString("sms_masteryPanel", "confirmText");
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
            Settings.spendMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
            Settings.advanceMasteryLevel(spec);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(UPGRADE_CONFIRMED_STR + Settings.getMasteryLevel(spec), Misc.getStoryBrightColor());
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
            }, Settings.doubleClickInterval);
        }
    }

    void beginConfirm(ButtonAPI button) {
        button.setCustomData(true);
        button.setText(UPGRADE_ASK_STR);
    }

    void endConfirm(ButtonAPI button) {
        button.setCustomData(false);
        button.setText(defaultText);
    }

    boolean isConfirming(ButtonAPI button) {
        return button.getCustomData() != null && (boolean) button.getCustomData();
    }
}
