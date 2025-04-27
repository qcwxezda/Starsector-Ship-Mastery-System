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
import java.util.Objects;

public class MasteryEffectButtonPressed extends ActionListener {
    final MasteryDisplay masteryDisplay;
    final ShipHullSpecAPI spec;
    final int level;
    final String optionId;

    public MasteryEffectButtonPressed(MasteryDisplay masteryDisplay, ShipHullSpecAPI spec, int level, String optionId) {
        this.masteryDisplay = masteryDisplay;
        this.spec = spec;
        this.level = level;
        this.optionId = optionId;
    }
    @Override
    public void trigger(Object... args) {
        ButtonAPI button = (ButtonAPI) args[1];
        List<MasteryEffect> effects = ShipMastery.getMasteryEffects(spec, level, optionId);
        boolean canDeactivate = true;
        for (MasteryEffect effect : effects) {
            if (!MasteryUtils.canDisable(effect)) {
                canDeactivate = false;
            }
        }

        NavigableMap<Integer, String> activeMasteries = ShipMastery.getPlayerActiveMasteriesCopy(spec);

        if (button.isChecked()) {
            // If the other option is selected and can't be disabled, disallow selecting of this one
            String active = activeMasteries.get(level);
            if (active != null && !active.equals(optionId)) {
                List<MasteryEffect> otherOptionEffects = ShipMastery.getMasteryEffects(spec, level, active);
                for (MasteryEffect effect : otherOptionEffects) {
                    if (!MasteryUtils.canDisable(effect)) {
                        button.setChecked(false);
                        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                                Strings.Misc.effectCantBeDeactivated,
                                Misc.getNegativeHighlightColor());
                        Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
                        return;
                    }
                }
            }

            masteryDisplay.deselectMasteryItem(level);
            masteryDisplay.selectMasteryItem(level, optionId);
            button.setHighlightBrightness(0f);

            if (!canDeactivate) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.Misc.effectCantBeDeactivatedWarning,
                        Misc.getNegativeHighlightColor());
            }
        }
        else {
            if (Objects.equals(optionId, activeMasteries.get(level))  && !canDeactivate) {
                button.setChecked(true);
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.Misc.effectCantBeDeactivated,
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
