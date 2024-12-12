package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.ui.triggers.ConstructButtonPressed;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class CreateConstructDisplay implements CustomUIElement {

    final ShipHullSpecAPI spec;
    final MasteryPanel panel;

    public CreateConstructDisplay(MasteryPanel panel, ShipHullSpecAPI spec) {
        this.spec = spec;
        this.panel = panel;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        tooltip.setParaOrbitronLarge();
        tooltip.setButtonFontOrbitron20();
        LabelAPI upgradeLabel = tooltip.addPara(Strings.MasteryPanel.createConstruct, 0f);
        upgradeLabel.setAlignment(Alignment.MID);
        upgradeLabel.getPosition().setXAlignOffset(5f);
        String buttonText = KnowledgeConstructPlugin.NUM_POINTS_GAINED + " MP";
        ButtonAPI constructButton =
                tooltip.addButton(buttonText, null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(),
                                     Alignment.MID, CutStyle.TL_BR, 200f, 25f, 5f);
        ReflectionUtils.setButtonListener(constructButton, new ConstructButtonPressed(panel, spec));
        constructButton.setEnabled(Global.getSettings().isDevMode() ||
                (ShipMastery.getPlayerMasteryPoints(spec) >= KnowledgeConstructPlugin.NUM_POINTS_GAINED)
        );
        constructButton.getPosition().setXAlignOffset(-5f);
    }
}
