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
import java.util.List;

public class RerollButtonPressed extends ActionListener {
    final MasteryPanel masteryPanel;
    final String defaultText;
    final ShipHullSpecAPI spec;

    public RerollButtonPressed(MasteryPanel masteryPanel, String defaultText, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.defaultText = defaultText;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {

        List<String> affectedLevels = new ArrayList<>();
        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
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
                affectedLevels.add("" + i);
            }
        }

        int mpCost = MasteryUtils.getRerollMPCost(spec);
        int spCost = MasteryUtils.getRerollSPCost(spec);
        String levelsJoined = Utils.joinStringList(affectedLevels);
        int curSP = Global.getSector().getPlayerStats().getStoryPoints();

        ReflectionUtils.GenericDialogData dialogData = ReflectionUtils.showGenericDialog(
                String.format(Strings.MasteryPanel.rerollMasteryConfirmText,
                        levelsJoined,
                        mpCost,
                        spCost,
                        curSP),
                Strings.MasteryPanel.confirmText2,
                Strings.MasteryPanel.cancelText,
                500f,
                250f,
                new ConfirmRerollMasteries(masteryPanel, spec)
        );

        if (dialogData != null) {
            dialogData.textLabel.setAlignment(Alignment.TMID);
            dialogData.textLabel.setHighlight(levelsJoined, mpCost + " MP", spCost + " SP", curSP + " SP");
            dialogData.textLabel.setHighlightColors(Settings.MASTERY_COLOR, Settings.MASTERY_COLOR, Misc.getStoryBrightColor(), Misc.getStoryBrightColor());
        }
    }
}
