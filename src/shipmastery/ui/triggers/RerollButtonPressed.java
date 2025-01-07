package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
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
            List<MasteryGenerator> gens = new ArrayList<>(ShipMastery.getGenerators(spec, i, false));
            gens.addAll(ShipMastery.getGenerators(spec, i, true));
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
                noEffect ? String.format(Strings.MasteryPanel.rerollMasteryNotApplicableText, levelsJoinedA) :
                        String.format(Strings.MasteryPanel.rerollMasteryConfirmText,
                        levelsJoinedA,
                        levelsJoinedB,
                        mpCost,
                        spCost,
                        curSP),
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                500f,
                noEffect ? 330f : 390f,
                new ConfirmRerollMasteries(masteryPanel, spec, noEffect)
        );

        if (dialogData != null) {
            dialogData.textLabel.setAlignment(Alignment.TMID);
            if (!noEffect) {
                dialogData.textLabel.setHighlight(levelsJoinedA, levelsJoinedB, mpCost + " MP", spCost + " SP", curSP + " SP");
                dialogData.textLabel.setHighlightColors(Settings.MASTERY_COLOR, Settings.MASTERY_COLOR, Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
            }
            else {
                dialogData.textLabel.setHighlight(levelsJoinedA);
                dialogData.textLabel.setHighlightColors(Settings.MASTERY_COLOR);
                dialogData.confirmButton.setEnabled(false);
            }
        }
    }
}
