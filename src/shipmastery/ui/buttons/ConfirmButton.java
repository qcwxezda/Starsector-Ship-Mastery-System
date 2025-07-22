package shipmastery.ui.buttons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import shipmastery.ShipMastery;
import shipmastery.util.Strings;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

public class ConfirmButton extends ButtonWithIcon {

    private final ShipHullSpecAPI spec;
    private final Map<Integer, String> selectedMasteries;

    public ConfirmButton(ShipHullSpecAPI spec,  Map<Integer, String> selectedMasteries) {
        super("graphics/icons/ui/sms_confirm_icon.png", false);
        this.spec = spec;
        this.selectedMasteries = selectedMasteries;
    }

    @Override
    public void onClick() {
        Map<Integer, String> activeSet = ShipMastery.getPlayerActiveMasteriesCopy(spec);
        Map<Integer, String> newSet = selectedMasteries;
        NavigableMap<Integer, String> toActivate = new TreeMap<>();
        NavigableMap<Integer, String> toDeactivate = new TreeMap<>();
        for (Map.Entry<Integer, String> entry : activeSet.entrySet()) {
            int level = entry.getKey();
            String active = entry.getValue();
            if (!newSet.containsKey(level) || !Objects.equals(active, newSet.get(level))) {
                toDeactivate.put(level, active);
            }
        }
        for (Map.Entry<Integer, String> entry : newSet.entrySet()) {
            int level = entry.getKey();
            String newId = entry.getValue();
            if (!activeSet.containsKey(level) || !Objects.equals(activeSet.get(level), newId)) {
                toActivate.put(level, newId);
            }
        }
        for (Map.Entry<Integer, String> entry : toDeactivate.descendingMap().entrySet()) {
            ShipMastery.deactivatePlayerMastery(spec, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, String> entry : toActivate.entrySet()) {
            ShipMastery.activatePlayerMastery(spec, entry.getKey(), entry.getValue());
        }
        Global.getSoundPlayer().playUISound("sms_change_masteries", 1f, 1f);

        finish();
    }

    @Override
    public String getTooltipTitle() {
        return Strings.MasteryPanel.confirmChangesTooltipTitle;
    }

    @Override
    public void appendToTooltip(TooltipMakerAPI tooltip) {
        tooltip.setParaFont(Fonts.ORBITRON_12);
        String key = Keyboard.getKeyName(Keyboard.KEY_SPACE).toLowerCase();
        tooltip.addPara(Strings.MasteryPanel.hotkey, 0f, Misc.getGrayColor(), darkHighlightColor, key);
        tooltip.setParaFontDefault();
        tooltip.addPara(Strings.MasteryPanel.confirmChangesTooltipText, 10f);
    }
}
