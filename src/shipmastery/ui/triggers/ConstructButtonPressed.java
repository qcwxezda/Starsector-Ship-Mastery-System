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
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;

public class ConstructButtonPressed extends ActionListener {

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    private ButtonAPI min, less, more, max;
    private LabelAPI confirmLabel;
    private LabelAPI spInfoLabel;
    private final IntRef count = new IntRef(1);
    private final int maxCount;
    private final IntRef spGained = new IntRef();
    public static final int CONSTRUCTS_PER_SP = 20;
    public static final String CONSTRUCTS_MADE_KEY = "$sms_constructsMade";

    public ConstructButtonPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
        maxCount = Math.max(1, (int) (ShipMastery.getPlayerMasteryPoints(spec) / MasteryUtils.getConstructCost()));
    }

    @Override
    public void trigger(Object... args) {
        String amount = MasteryUtils.getConstructCost() + " MP";
        float credits = KnowledgeConstructPlugin.getPrice(spec);
        String creditsStr = Misc.getDGSCredits(credits);

        int constructsMade = (int) Global.getSector().getPersistentData().getOrDefault(CONSTRUCTS_MADE_KEY, 0);

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.Misc.confirm,
                Strings.Misc.cancel,
                550f,
                170f,
                new ConfirmCreateConstruct(masteryPanel, spec, count, spGained)
        );

        if (dialogData != null) {
            CustomPanelAPI selector = Global.getSettings().createCustom(550f, 100f, null);
            TooltipMakerAPI tooltip = selector.createUIElement(525f, 50f, false);
            tooltip.setParaInsigniaLarge();
            confirmLabel = tooltip.addPara(String.format(Strings.MasteryPanel.createConstructConfirmTextSingular, amount, 1, creditsStr), 5f);
            confirmLabel.setHighlight(amount, creditsStr);
            confirmLabel.setHighlightColors(Settings.MASTERY_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);
            boolean nextConstructGrantsSP = (constructsMade+1) % CONSTRUCTS_PER_SP == 0;
            spGained.value = nextConstructGrantsSP ? 1 : 0;
            tooltip.setParaFontDefault();
            spInfoLabel = tooltip.addPara(
                    String.format(
                            nextConstructGrantsSP ?
                                    Strings.MasteryPanel.createConstructSPTextSingular :
                                    Strings.MasteryPanel.createConstructSPTextPlural,
                            CONSTRUCTS_PER_SP,
                            "" + spGained.value),
                    25f);
            spInfoLabel.setHighlight("" + CONSTRUCTS_PER_SP, "" + spGained.value);
            spInfoLabel.setHighlightColors(Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getStoryBrightColor());
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

            selector.addUIElement(buttons).inBMid(-155f);
            dialogData.panel.addComponent(selector).inTL(0f, 0f);
            updatePanel();
        }
    }

    private void updatePanel() {
        String amount = (count.value * MasteryUtils.getConstructCost()) + " MP";
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

        int constructsMade = (int) Global.getSector().getPersistentData().getOrDefault(CONSTRUCTS_MADE_KEY, 0);
        spGained.value = count.value / CONSTRUCTS_PER_SP;
        int rem = count.value % CONSTRUCTS_PER_SP;
        if (constructsMade % CONSTRUCTS_PER_SP + rem >= CONSTRUCTS_PER_SP) {
            spGained.value++;
        }

        spInfoLabel.setText(String.format(
                spGained.value == 1 ? Strings.MasteryPanel.createConstructSPTextSingular : Strings.MasteryPanel.createConstructSPTextPlural,
                CONSTRUCTS_PER_SP,
                spGained.value
        ));
        spInfoLabel.setHighlight("" + CONSTRUCTS_PER_SP, "" + spGained.value);
        spInfoLabel.setHighlightColors(Settings.POSITIVE_HIGHLIGHT_COLOR, Misc.getStoryBrightColor());

        min.setEnabled(count.value != 1);
        less.setEnabled(count.value > 1);
        more.setEnabled(count.value < maxCount);
        max.setEnabled(count.value != maxCount);
    }
}
