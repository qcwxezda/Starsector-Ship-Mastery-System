package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.mastery.MasteryEffect;
import shipmastery.ui.MasteryDisplay;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;

import java.util.List;
import java.util.NavigableMap;

public class MasteryEffectButtonPressed extends ActionListener {
    MasteryDisplay masteryDisplay;
    ShipHullSpecAPI spec;
    int level;
    boolean isOption2;

    public MasteryEffectButtonPressed(MasteryDisplay masteryDisplay, ShipHullSpecAPI spec, int level, boolean isOption2) {
        this.masteryDisplay = masteryDisplay;
        this.spec = spec;
        this.level = level;
        this.isOption2 = isOption2;
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
        List<MasteryEffect> effects = ShipMastery.getMasteryEffects(spec, level, isOption2);
        boolean canDeactivate = true;
        for (MasteryEffect effect : effects) {
            if (!MasteryUtils.canDisable(effect)) {
                canDeactivate = false;
            }
        }

        NavigableMap<Integer, Boolean> activeMasteries = ShipMastery.getPlayerActiveMasteriesCopy(spec);

        if (button.isChecked()) {
            // If the other option is selected and can't be disabled, disallow selecting of this one
            Boolean active = activeMasteries.get(level);
            if (active != null && active == !isOption2) {
                List<MasteryEffect> otherOptionEffects = ShipMastery.getMasteryEffects(spec, level, active);
                for (MasteryEffect effect : otherOptionEffects) {
                    if (!MasteryUtils.canDisable(effect)) {
                        button.setChecked(false);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                                Strings.EFFECT_CANT_DEACTIVATE,
                                Misc.getNegativeHighlightColor());
                        Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
                        return;
                    }
                }
            }

            masteryDisplay.deselectMasteryItem(level);
            masteryDisplay.selectMasteryItem(level, isOption2);
            button.setHighlightBrightness(0f);

            if (!canDeactivate) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.EFFECT_CANT_DEACTIVATE_WARNING,
                        Misc.getNegativeHighlightColor());
            }
        }
        else {
            if ((Boolean) isOption2 == activeMasteries.get(level) && !canDeactivate) {
                button.setChecked(true);
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.EFFECT_CANT_DEACTIVATE,
                        Misc.getNegativeHighlightColor());
                Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
            }
            else {
                masteryDisplay.deselectMasteryItem(level);
                button.setHighlightBrightness(0.25f);
            }
        }
    }
}
