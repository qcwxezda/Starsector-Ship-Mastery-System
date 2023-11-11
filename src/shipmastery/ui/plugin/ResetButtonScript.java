package shipmastery.ui.plugin;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import shipmastery.Settings;

import java.util.List;

public class ResetButtonScript implements CustomUIPanelPlugin {
    ButtonAPI resetButton;
    float confirmTime = 0f;
    String defaultText;
    String confirmText = "Confirm?";

    public void setButton(ButtonAPI button) {
        resetButton = button;
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
        if (resetButton == null) return;
        boolean confirming = (boolean) resetButton.getCustomData();

        if (confirming) {
            if (defaultText.equals(resetButton.getText())) {
                resetButton.setText(confirmText);
                confirmTime = 0f;
            }

            confirmTime += amount;
            if (confirmTime > Settings.doubleClickInterval) {
                resetButton.setCustomData(false);
                resetButton.setText(defaultText);
            }
        }
    }

    @Override
    public void processInput(List<InputEventAPI> list) {}

    @Override
    public void buttonPressed(Object o) {
        if (o instanceof ButtonAPI) {
            System.out.println("Button pressed: " + ((ButtonAPI) o).getText());
        } else {
            System.out.println("Button pressed: " + o);
        }
    }
}
