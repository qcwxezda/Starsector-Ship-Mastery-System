package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.deferred.Action;
import shipmastery.deferred.DeferredActionPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.Strings;

import java.util.ArrayList;

public class ClearSModsPressed extends ActionListener {

    MasteryPanel masteryPanel;
    ShipAPI module;
    String defaultText;

    public ClearSModsPressed(MasteryPanel masteryPanel, ShipAPI module, String defaultText) {
        this.masteryPanel = masteryPanel;
        this.module = module;
        this.defaultText = defaultText;
    }

    @Override
    public void trigger(Object... args) {
        final ButtonAPI button = (ButtonAPI) args[1];
        ShipVariantAPI variant = module.getVariant();
        if (isConfirming(button)) {
            endConfirm(button);

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
            masteryPanel.forceRefresh(true, true, true);

            // Some non-s-modded hullmods may no longer be applicable; remove these also
            // Do-while loop must terminate; variant's hullmod count is decrementing
            boolean changed;
            do {
                changed = false;
                for (String id : variant.getNonBuiltInHullmods()) {
                    HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
                    if (spec.getEffect() != null && !spec.getEffect().isApplicableToShip(module)) {
                        variant.removeMod(id);
                        changed = true;
                    }
                }
                if (changed) {
                    masteryPanel.forceRefresh(true, true, true);
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
