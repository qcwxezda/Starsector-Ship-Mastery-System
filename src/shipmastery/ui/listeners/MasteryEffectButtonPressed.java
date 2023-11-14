package shipmastery.ui.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.MasteryEffect;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;

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
        ButtonAPI button = (ButtonAPI) args[1];
        MasteryEffect effect = MasteryUtils.getMasteryEffect(spec, level);

        if (button.isChecked()) {
            masteryPanel.selectMasteryItem(level);
            button.setHighlightBrightness(0f);

            if (!effect.canBeDeactivated()) {
                Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                        Strings.EFFECT_CANT_DEACTIVATE_WARNING,
                        Misc.getNegativeHighlightColor());
            }
        }
        else {
            if (MasteryUtils.getActiveMasteries(spec).contains(level) && !effect.canBeDeactivated()) {
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
