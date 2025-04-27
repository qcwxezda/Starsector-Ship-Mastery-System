package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
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
        ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
        ShipMastery.advancePlayerMasteryLevel(spec);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                Strings.MasteryPanel.upgradeConfirm + ShipMastery.getPlayerMasteryLevel(spec), Settings.MASTERY_COLOR);
        Global.getSoundPlayer().playUISound("sms_increase_mastery", 1f, 1f);
        masteryPanel.forceRefresh(true, false, false, false);

        Utils.fixPlayerFleetInconsistencies();
    }
}
