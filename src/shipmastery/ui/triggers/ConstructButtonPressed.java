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
    private final IntRef remainder = new IntRef();
    public static final int INITIAL_CONSTRUCTS_PER_SP = 10;
    public static final int MAX_CONSTRUCTS_PER_SP = 40;
    public static final int CONSTRUCTS_PER_SP_INCREMENT = 1;

    public static final String CONSTRUCTS_MADE_KEY = "$sms_constructsMadeV2";
    public static final String CONSTRUCTS_NEEDED_KEY = "$sms_constructsNeeded";

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
        int constructsNeeded = (int) Global.getSector().getPersistentData().getOrDefault(CONSTRUCTS_NEEDED_KEY, INITIAL_CONSTRUCTS_PER_SP);

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.Misc.confirm,
                Strings.Misc.cancel,
                550f,
                185f,
                new ConfirmCreateConstruct(masteryPanel, spec, count, spGained, remainder)
        );

        if (dialogData != null) {
            CustomPanelAPI selector = Global.getSettings().createCustom(550f, 100f, null);
            TooltipMakerAPI tooltip = selector.createUIElement(525f, 50f, false);
            tooltip.setParaInsigniaLarge();
            confirmLabel = tooltip.addPara(String.format(Strings.MasteryPanel.createConstructConfirmTextSingular, amount, 1, creditsStr), 5f);
            confirmLabel.setHighlight(amount, creditsStr);
            confirmLabel.setHighlightColors(Settings.MASTERY_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);
            boolean nextConstructGrantsSP = constructsMade + 1 == constructsNeeded;
            spGained.value = nextConstructGrantsSP ? 1 : 0;
            remainder.value = nextConstructGrantsSP ? 0 : constructsMade + 1;
            tooltip.setParaFontDefault();
            spInfoLabel = tooltip.addPara(
                    String.format(
                            nextConstructGrantsSP ?
                                    Strings.MasteryPanel.createConstructSPTextSingular :
                                    Strings.MasteryPanel.createConstructSPTextPlural,
                            spGained.value,
                            constructsNeeded - remainder.value,
                            constructsNeeded - remainder.value == 1 ? Strings.MasteryPanel.constructSinglar : Strings.MasteryPanel.constructPlural,
                            CONSTRUCTS_PER_SP_INCREMENT,
                            MAX_CONSTRUCTS_PER_SP),
                    25f);
            spInfoLabel.setHighlight("" + spGained.value, "" + (constructsNeeded - remainder.value), "" + CONSTRUCTS_PER_SP_INCREMENT, "" + MAX_CONSTRUCTS_PER_SP);
            spInfoLabel.setHighlightColors(Misc.getStoryBrightColor(), Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);
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

            selector.addUIElement(buttons).inBMid(-170f);
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
        int constructsNeeded = (int) Global.getSector().getPersistentData().getOrDefault(CONSTRUCTS_NEEDED_KEY, INITIAL_CONSTRUCTS_PER_SP);
        spGained.value = 0;

        constructsMade += count.value;
        while (constructsMade >= constructsNeeded) {
            constructsMade -= constructsNeeded;
            spGained.value++;
            constructsNeeded += CONSTRUCTS_PER_SP_INCREMENT;
            constructsNeeded = Math.min(constructsNeeded, MAX_CONSTRUCTS_PER_SP);
        }
        remainder.value = constructsMade;

        spInfoLabel.setText(String.format(
                spGained.value == 1 ? Strings.MasteryPanel.createConstructSPTextSingular : Strings.MasteryPanel.createConstructSPTextPlural,
                spGained.value,
                constructsNeeded - remainder.value,
                constructsNeeded - remainder.value == 1 ? Strings.MasteryPanel.constructSinglar : Strings.MasteryPanel.constructPlural,
                CONSTRUCTS_PER_SP_INCREMENT,
                MAX_CONSTRUCTS_PER_SP
        ));
        spInfoLabel.setHighlight("" + spGained.value, "" + (constructsNeeded - remainder.value), "" + CONSTRUCTS_PER_SP_INCREMENT, "" + MAX_CONSTRUCTS_PER_SP);
        spInfoLabel.setHighlightColors(Misc.getStoryBrightColor(), Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR);

        min.setEnabled(count.value != 1);
        less.setEnabled(count.value > 1);
        more.setEnabled(count.value < maxCount);
        max.setEnabled(count.value != maxCount);
    }
}
