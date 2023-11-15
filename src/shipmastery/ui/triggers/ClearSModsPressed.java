package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.Action;
import shipmastery.campaign.DeferredActionPlugin;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.Strings;

import java.util.ArrayList;

public class ClearSModsPressed extends ActionListener {

    MasteryPanel masteryPanel;
    String defaultText;

    public ClearSModsPressed(MasteryPanel masteryPanel, String defaultText) {
        this.masteryPanel = masteryPanel;
        this.defaultText = defaultText;
    }

    @Override
    public void trigger(Object... args) {
        final ButtonAPI button = (ButtonAPI) args[1];
        if (isConfirming(button)) {
            endConfirm(button);

            ShipVariantAPI variant = masteryPanel.getShip().getVariant();
            int removedCount = 0;
            // Copy required as removePermaMod also calls getSMods().remove()
            for (String id : new ArrayList<>(variant.getSMods())) {
                variant.removePermaMod(id);
                removedCount++;
            }

            if (removedCount == 0) {
                return;
            }

            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(Strings.CLEAR_CONFIRMED_STR, Misc.getStoryBrightColor());
            Global.getSoundPlayer().playUISound("sms_clear_smods", 1f, 1f);
            masteryPanel.forceRefresh(true, true);

            // Some non-s-modded hullmods may no longer be applicable; remove these also
            // Do-while loop must terminate; variant's hullmod count is decrementing
            boolean changed;
            do {
                changed = false;
                for (String id : variant.getNonBuiltInHullmods()) {
                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
                    if (spec.getEffect() != null && !spec.getEffect().isApplicableToShip(masteryPanel.getShip())) {
                        variant.removeMod(id);
                        changed = true;
                    }
                }
                if (changed) {
                    masteryPanel.forceRefresh(true, true);
                }
            } while (changed);
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
        button.setText(Strings.CLEAR_ASK_STR);
    }

    void endConfirm(ButtonAPI button) {
        button.setCustomData(false);
        button.setText(defaultText);
    }

    boolean isConfirming(ButtonAPI button) {
        return button.getCustomData() != null && (boolean) button.getCustomData();
    }
}
