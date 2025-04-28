package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.IntRef;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class ConstructButtonPressed extends ActionListener {

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    private ButtonAPI min, less, more, max;
    private LabelAPI confirmLabel;
    private final IntRef count = new IntRef();
    private final int maxCount;

    public ConstructButtonPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
        maxCount = Math.max(1, (int) (ShipMastery.getPlayerMasteryPoints(spec) / 10f));
    }

    @Override
    public void trigger(Object... args) {
        String amount = KnowledgeConstructPlugin.NUM_POINTS_GAINED + " MP";
        float credits = KnowledgeConstructPlugin.getPrice(spec);
        String creditsStr = Misc.getDGSCredits(credits);

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                500f,
                150f,
                new ConfirmCreateConstruct(masteryPanel, spec, count)
        );

        if (dialogData != null) {
            CustomPanelAPI selector = Global.getSettings().createCustom(500f, 100f, null);
            TooltipMakerAPI tooltip = selector.createUIElement(475f, 50f, false);
            tooltip.setParaInsigniaLarge();
            confirmLabel = tooltip.addPara(String.format(Strings.MasteryPanel.createConstructConfirmTextSingular, amount, 1, creditsStr), 0f);
            confirmLabel.setHighlight(amount, creditsStr);
            confirmLabel.setHighlightColors(Settings.MASTERY_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);
            selector.addUIElement(tooltip).inTL(8f, 8f);
            TooltipMakerAPI buttons = selector.createUIElement(200f, 50f, false);
            buttons.setButtonFontOrbitron20();
            min = buttons.addButton("--", null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.NONE, 30f, 30f, -30f);
            min.getPosition().setXAlignOffset(0f);
            ReflectionUtils.setButtonListener(min, new ActionListener() {
                @Override
                public void trigger(Object... args) {
                    count.value = 1;
                    updatePanel();
                }
            });

            less = buttons.addButton("-", null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.NONE, 30f, 30f, -30f);
            less.getPosition().setXAlignOffset(40f);
            ReflectionUtils.setButtonListener(less, new ActionListener() {
                @Override
                public void trigger(Object... args) {
                    count.value--;
                    updatePanel();
                }
            });

            more = buttons.addButton("+", null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.NONE, 30f, 30f, -30f);
            more.getPosition().setXAlignOffset(40f);
            ReflectionUtils.setButtonListener(more, new ActionListener() {
                @Override
                public void trigger(Object... args) {
                    count.value++;
                    updatePanel();
                }
            });

            max = buttons.addButton("++", null, Misc.getBrightPlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, CutStyle.NONE, 30f, 30f, -30f);
            max.getPosition().setXAlignOffset(40f);
            ReflectionUtils.setButtonListener(max, new ActionListener() {
                @Override
                public void trigger(Object... args) {
                    count.value = maxCount;
                    updatePanel();
                }
            });

            selector.addUIElement(buttons).inBMid(-135f);
            dialogData.panel.addComponent(selector).inTL(0f, 0f);
            updatePanel();
        }
    }

    private void updatePanel() {
        String amount = (count.value * KnowledgeConstructPlugin.NUM_POINTS_GAINED) + " MP";
        int credits = count.value * KnowledgeConstructPlugin.getPrice(spec);
        String creditsStr = Misc.getDGSCredits(credits);
        confirmLabel.setText(String.format(
                count.value == 1 ? Strings.MasteryPanel.createConstructConfirmTextSingular
                        : Strings.MasteryPanel.createConstructConfirmTextPlural,
                amount,
                count.value,
                creditsStr));
        confirmLabel.setHighlight(amount, creditsStr);
        confirmLabel.setHighlightColors(Settings.MASTERY_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);

        min.setEnabled(count.value != 1);
        less.setEnabled(count.value > 1);
        more.setEnabled(count.value < maxCount);
        max.setEnabled(count.value != maxCount);
    }
}
