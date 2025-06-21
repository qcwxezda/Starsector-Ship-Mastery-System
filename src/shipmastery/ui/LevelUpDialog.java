package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.ShipMastery;
import shipmastery.util.ReflectionUtils;

public class LevelUpDialog {

    private final ShipHullSpecAPI spec;

    public LevelUpDialog(ShipHullSpecAPI spec) {
        this.spec = spec;
    }

    public void show() {
        float height = 600f;
        float width = 1000f;
        float shipDisplaySize = 200f;

        ReflectionUtils.GenericDialogData data = ReflectionUtils.showGenericDialog("", "Confirm", "Cancel", width, height, null);
        if (data == null) return;

        CustomPanelAPI panel = Global.getSettings().createCustom(width, height, null);
        TooltipMakerAPI shipDisplay = panel.createUIElement(shipDisplaySize, shipDisplaySize+25f, false);
        new ShipDisplay(spec, shipDisplaySize).create(shipDisplay);
        panel.addUIElement(shipDisplay).inLMid(10f);

        int currentLevel = ShipMastery.getPlayerMasteryLevel(spec);
        TooltipMakerAPI levelUpTitle = panel.createUIElement(width-shipDisplaySize, height, false);
        levelUpTitle.setTitleFont(Fonts.ORBITRON_20AA);
        levelUpTitle.addTitle(String.format("Select a perk for level %s", currentLevel+1)).setAlignment(Alignment.MID);
        panel.addUIElement(levelUpTitle).inTMid(30f);

        TooltipMakerAPI outline = panel.createUIElement(width-shipDisplaySize-100f, height-100f, false);
        new MasteryDisplayOutline(width-shipDisplaySize-100f, height-100f).create(outline);
        panel.addUIElement(outline).inTR(60f, 60f);

        data.panel.addComponent(panel);
    }
}
