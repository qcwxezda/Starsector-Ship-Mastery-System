package shipmastery.ui;

import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ui.triggers.ActionListener;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class ConfirmOrCancelDisplay implements CustomUIElement {

    public ButtonAPI confirmButton;
    public ButtonAPI cancelButton;

    ActionListener onConfirm;
    ActionListener onCancel;

    public ConfirmOrCancelDisplay(ActionListener onConfirm, ActionListener onCancel) {
        this.onCancel = onCancel;
        this.onConfirm = onConfirm;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        tooltip.setParaFont(Fonts.ORBITRON_16);
        tooltip.addPara(Strings.CHANGES_PENDING, 10f).setAlignment(Alignment.MID);
        tooltip.setButtonFontOrbitron20();
        confirmButton = tooltip.addButton(Strings.CONFIRM_STR, null, 100f, 25f, 20f);
        ReflectionUtils.setButtonListener(confirmButton, onConfirm);
        cancelButton = tooltip.addButton(Strings.CANCEL_STR, null, 100f, 25f, -25f);
        cancelButton.getPosition().setXAlignOffset(120f);
        ReflectionUtils.setButtonListener(cancelButton, onCancel);
    }
}
