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
import shipmastery.ui.triggers.RerollButtonPressed;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class RerollMasteryDisplay implements CustomUIElement {

    final ShipHullSpecAPI spec;
    final MasteryPanel panel;

    public RerollMasteryDisplay(MasteryPanel panel, ShipHullSpecAPI spec) {
        this.spec = spec;
        this.panel = panel;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        tooltip.setParaOrbitronLarge();
        tooltip.setButtonFontOrbitron20();
        LabelAPI rerollLabel = tooltip.addPara(Strings.MasteryPanel.rerollMasteries, 0f);
        rerollLabel.setAlignment(Alignment.MID);
        rerollLabel.getPosition().setXAlignOffset(5f);
        int cost = MasteryUtils.getRerollMPCost(spec);
        int spCost = MasteryUtils.getRerollSPCost(spec);
        String buttonText = cost + " MP + " + spCost + " SP";
        ButtonAPI rerollButton =
                tooltip.addButton(buttonText, null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(),
                                     Alignment.MID, CutStyle.TL_BR, 200f, 25f, 5f);
        ReflectionUtils.setButtonListener(rerollButton, new RerollButtonPressed(panel, spec));
        rerollButton.setEnabled(Global.getSettings().isDevMode() ||
                (ShipMastery.getPlayerMasteryPoints(spec) >= cost && Global.getSector().getPlayerStats().getStoryPoints() >= spCost)
        );
        rerollButton.getPosition().setXAlignOffset(-5f);
    }
}
