package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.mastery.MasteryEffect;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;

import java.util.List;

public class MasteryEffectButtonPressed extends ActionListener {
    MasteryPanel masteryPanel;
    ShipHullSpecAPI spec;
    int level;

    public MasteryEffectButtonPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec, int level) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
        this.level = level;
    }
    @Override
    public void trigger(Object... args) {

//        if (!masteryPanel.isInRestorableMarket()) {
//            Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
//            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
//                    Strings.MUST_BE_DOCKED_MASTERIES,
//                    Misc.getNegativeHighlightColor());
//            return;
//        }

        ButtonAPI button = (ButtonAPI) args[1];
        List<MasteryEffect> effects = ShipMastery.getMasteryEffects(spec, level);
        boolean canDeactivate = true;
        for (MasteryEffect effect : effects) {
            if (!MasteryUtils.canDisable(effect)) {
                canDeactivate = false;
            }
        }

        if (button.isChecked()) {
            masteryPanel.selectMasteryItem(level);
            button.setHighlightBrightness(0f);

            if (!canDeactivate) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.EFFECT_CANT_DEACTIVATE_WARNING,
                        Misc.getNegativeHighlightColor());
            }
        }
        else {
            if (ShipMastery.getActiveMasteries(spec).contains(level) && !canDeactivate) {
                button.setChecked(true);
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.EFFECT_CANT_DEACTIVATE,
                        Misc.getNegativeHighlightColor());
                Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
            }
            else {
                masteryPanel.deselectMasteryItem(level);
                button.setHighlightBrightness(0.25f);
            }
        }
    }
}
