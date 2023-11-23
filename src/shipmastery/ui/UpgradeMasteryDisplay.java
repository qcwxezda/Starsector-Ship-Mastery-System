package shipmastery.ui;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.ui.triggers.UpgradeButtonPressed;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class UpgradeMasteryDisplay implements CustomUIElement {

    ShipHullSpecAPI spec;
    MasteryPanel panel;

    public UpgradeMasteryDisplay(MasteryPanel panel, ShipHullSpecAPI spec) {
        this.spec = spec;
        this.panel = panel;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        tooltip.setParaOrbitronLarge();
        tooltip.setButtonFontOrbitron20();
        LabelAPI upgradeLabel = tooltip.addPara(Strings.ADVANCE_MASTERY_STR, 0f);
        upgradeLabel.setAlignment(Alignment.MID);
        upgradeLabel.getPosition().setXAlignOffset(5f);
        int cost = MasteryUtils.getUpgradeCost(spec);
        String buttonText = "" + cost + " MP";
        ButtonAPI upgradeButton =
                tooltip.addButton(buttonText, null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(),
                                     Alignment.MID, CutStyle.TL_BR, 150f, 25f, 5f);
        ReflectionUtils.setButtonListener(upgradeButton, new UpgradeButtonPressed(panel, buttonText, spec));
        upgradeButton.setEnabled(ShipMastery.getPlayerMasteryPoints(spec) >= cost);
        upgradeButton.getPosition().setXAlignOffset(20f);
    }
}
