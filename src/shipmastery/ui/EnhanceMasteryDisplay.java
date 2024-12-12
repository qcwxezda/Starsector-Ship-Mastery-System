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
import shipmastery.ui.triggers.EnhanceButtonPressed;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class EnhanceMasteryDisplay implements CustomUIElement {

    final ShipHullSpecAPI spec;
    final MasteryPanel panel;

    public static final String ENHANCE_MAP = "$sms_EnhanceMap";
    public static final String ENHANCE_MODIFIER_ID = "sms_enhancement";

    public EnhanceMasteryDisplay(MasteryPanel panel, ShipHullSpecAPI spec) {
        this.spec = spec;
        this.panel = panel;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        tooltip.setParaOrbitronLarge();
        tooltip.setButtonFontOrbitron20();
        LabelAPI upgradeLabel = tooltip.addPara(Strings.MasteryPanel.enhanceMasteries, 0f);
        upgradeLabel.setAlignment(Alignment.MID);
        upgradeLabel.getPosition().setXAlignOffset(5f);
        int cost = MasteryUtils.getEnhanceMPCost(spec);
        int spCost = MasteryUtils.getEnhanceSPCost(spec);
        String buttonText = spCost > 0 ? cost + " MP + " + spCost + " SP" : cost + " MP";
        ButtonAPI enhanceButton =
                tooltip.addButton(buttonText, null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(),
                                     Alignment.MID, CutStyle.TL_BR, 200f, 25f, 5f);
        ReflectionUtils.setButtonListener(enhanceButton, new EnhanceButtonPressed(panel, spec));
        enhanceButton.setEnabled(Global.getSettings().isDevMode() ||
                (ShipMastery.getPlayerMasteryPoints(spec) >= cost && Global.getSector().getPlayerStats().getStoryPoints() >= spCost)
        );
        enhanceButton.getPosition().setXAlignOffset(-5f);
    }
}
