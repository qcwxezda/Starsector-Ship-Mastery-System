package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.data.MasteryGenerator;
import shipmastery.mastery.impl.random.RandomMastery;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.ReflectionUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RerollButtonPressed extends ActionListener {
    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    public RerollButtonPressed(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {

        Set<Integer> inactiveLevels = new HashSet<>();
        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            inactiveLevels.add(i);
        }
        inactiveLevels.removeAll(ShipMastery.getPlayerActiveMasteriesCopy(spec).keySet());

        List<Integer> randomizedLevels = new ArrayList<>();
        for (int i  = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            List<String> optionIds = ShipMastery.getMasteryOptionIds(spec, i);
            List<MasteryGenerator> gens = new ArrayList<>();
            for (String id : optionIds) {
                gens.addAll(ShipMastery.getGenerators(spec, i, id));
            }
            boolean affected = false;
            for (MasteryGenerator gen : gens) {
                if (RandomMastery.class.isAssignableFrom(gen.effectClass)) {
                    affected = true;
                    break;
                }
            }
            if (affected) {
                randomizedLevels.add(i);
            }
        }

        int mpCost = MasteryUtils.getRerollMPCost(spec);
        int spCost = MasteryUtils.getRerollSPCost(spec);

        String levelsJoinedA = Utils.joinList(randomizedLevels);
        randomizedLevels.retainAll(inactiveLevels);
        String levelsJoinedB = Utils.joinList(randomizedLevels);

        int curSP = Global.getSector().getPlayerStats().getStoryPoints();
        boolean noEffect = randomizedLevels.isEmpty();

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                "",
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                500f,
                300f,
                new ConfirmRerollMasteries(masteryPanel, spec, noEffect)
        );

        if (dialogData != null) {
            CustomPanelAPI inner = Global.getSettings().createCustom(500f, 300f, null);
            TooltipMakerAPI text = inner.createUIElement(475f, 200f, true);
            text.setParaInsigniaLarge();
            text.addPara(Strings.MasteryPanel.rerollMasteryConfirmText, 0f);
            text.setParaFontDefault();
            if (noEffect) {
                text.addPara(Strings.MasteryPanel.rerollMasteryComfirmTextNotApplicable, 10f, Settings.MASTERY_COLOR, levelsJoinedA);
                dialogData.confirmButton.setEnabled(false);
            }
            else {
                text.addPara(Strings.MasteryPanel.rerollMasteryConfirmTextApplicable, 10f, Settings.MASTERY_COLOR, levelsJoinedA, levelsJoinedB);
                TooltipMakerAPI costText = inner.createUIElement(475f, 25f, false);
                costText.setParaInsigniaLarge();
                costText.addPara(Strings.MasteryPanel.rerollMasteryConfirmTextApplicableCost,
                        10f,
                        new Color[] {Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor()},
                        "" + mpCost,
                        "" + spCost,
                        "" + curSP);
                inner.addUIElement(costText).inBL(8f, 65f);
            }
            inner.addUIElement(text).inTL(8f, 8f);
            dialogData.panel.addComponent(inner).inTL(0f, 0f);

            dialogData.textLabel.setAlignment(Alignment.TMID);
        }
    }
}
