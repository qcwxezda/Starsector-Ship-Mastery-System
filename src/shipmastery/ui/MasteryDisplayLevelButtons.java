package shipmastery.ui;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class MasteryDisplayLevelButtons implements CustomUIElement, CustomUIPanelPlugin {

    private final MasteryDisplay display;
    private final float buttonW, buttonH;
    private final float buttonPad;
    private final int numButtons;
    private final int buttonsPerRow;
    private final ShipHullSpecAPI spec;

    private final List<ButtonAPI> buttons = new ArrayList<>();
    private int checkedButtonIndex = -1;
    private final IntervalUtil updateInterval = new IntervalUtil(0.05f, 0.05f);

    MasteryDisplayLevelButtons(MasteryDisplay display, ShipHullSpecAPI spec, int numButtons, int buttonsPerRow, float buttonW, float buttonH, float buttonPad) {
        this.display = display;
        this.buttonW = buttonW;
        this.buttonH = buttonH;
        this.buttonPad = buttonPad;
        this.numButtons = numButtons;
        this.buttonsPerRow = buttonsPerRow;
        this.spec = spec;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        tooltip.setAreaCheckboxFont(buttonW < 36f ? Fonts.ORBITRON_12 : Fonts.ORBITRON_20AA);
        int playerLevel = ShipMastery.getPlayerMasteryLevel(spec);
        var playerActive = ShipMastery.getPlayerActiveMasteriesCopy(spec);

        int buttonsThisRow = 0;
        for (int i = 1; i <= numButtons; i++) {
            Color textColor;
            if (playerLevel < i) {
                textColor = Misc.getGrayColor();
            }
            else {
                textColor = Misc.getBrightPlayerColor();
            }

            ButtonAPI button = tooltip.addAreaCheckbox(
                    "" + i,
                    i,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    textColor,
                    buttonW,
                    buttonH,
                    -buttonH);

            if (buttonsThisRow >= 1) {
                button.getPosition().setXAlignOffset(buttonW + buttonPad);
            } else if (i != 1) {
                button.getPosition().setXAlignOffset(-(buttonW + buttonPad) * (buttonsPerRow - 1));
            }

            if (!playerActive.containsKey(i) && playerLevel >= i) {
                ReflectionUtils.invokeMethod(
                        ReflectionUtils.invokeMethod (button,"getAreaCheckboxButtonPanel"),
                        "setBorderOverride",
                        Utils.mixColor(Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getDarkPlayerColor(), 0.4f));
            }
            buttons.add(button);

            buttonsThisRow++;
            if (buttonsThisRow >= buttonsPerRow) {
                buttonsThisRow = 0;
                tooltip.addSpacer(buttonH);
            }
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {}

    @Override
    public void renderBelow(float alphaMult) {}

    @Override
    public void render(float alphaMult) {}

    @Override
    public void advance(float amount) {
        updateInterval.advance(amount);
        if (updateInterval.intervalElapsed()) {
            update();
        }
    }

    private void update() {
        if (checkedButtonIndex >= 0) {
            buttons.get(checkedButtonIndex).setChecked(false);
        }
        int nearest = display.getNearestLevelToScroller();
        if (nearest-1 >= 0 && nearest-1 < buttons.size()) {
            checkedButtonIndex = nearest-1;
            buttons.get(checkedButtonIndex).setChecked(true);
        }
    }

    @Override
    public void processInput(List<InputEventAPI> events) {}

    @Override
    public void buttonPressed(Object buttonId) {
        if (buttonId instanceof Integer) {
            display.scrollToLevel((int) buttonId);
            update();
        }
    }
}
