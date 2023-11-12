package shipmastery.ui.plugin;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import shipmastery.Settings;
import shipmastery.util.Utils;

import java.util.List;

public class ButtonWithConfirmScript implements CustomUIPanelPlugin {
    ButtonAPI button;
    float confirmTime = 0f;
    String defaultText;
    String confirmText = Utils.getString("sms_masteryPanel", "confirmText");

    public void setButton(ButtonAPI button) {
        this.button = button;
        defaultText = button.getText();
    }

    @Override
    public void positionChanged(PositionAPI positionAPI) {}

    @Override
    public void renderBelow(float v) {}

    @Override
    public void render(float v) {}

    @Override
    public void advance(float amount) {
        if (button == null) return;
        boolean confirming = (boolean) button.getCustomData();

        if (confirming) {
            if (defaultText.equals(button.getText())) {
                button.setText(confirmText);
                confirmTime = 0f;
            }

            confirmTime += amount;
            if (confirmTime > Settings.doubleClickInterval) {
                button.setCustomData(false);
                button.setText(defaultText);
            }
        }
    }

    @Override
    public void processInput(List<InputEventAPI> list) {}

    @Override
    public void buttonPressed(Object o) {}
}
