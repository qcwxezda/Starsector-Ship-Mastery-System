package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryEffect;
import shipmastery.plugin.ModPlugin;
import shipmastery.ui.MasteryPanel;
import shipmastery.ui.RerollMasteryDisplay;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class ConfirmRerollMasteries extends DialogDismissedListener{

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    public ConfirmRerollMasteries(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) return;

        ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getUpgradeCost(spec));
        Global.getSector().getPlayerStats().spendStoryPoints(MasteryUtils.getRerollSPCost(spec), false, null, false, 0f, null);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                Strings.MasteryPanel.rerollConfirm, Settings.MASTERY_COLOR);

        // Increment reroll count
        //noinspection unchecked
        Map<String, Integer> rerollMap = (Map<String, Integer>) Global.getSector().getPersistentData().get(RerollMasteryDisplay.REROLL_MAP);
        if (rerollMap == null) {
            rerollMap = new HashMap<>();
            Global.getSector().getPersistentData().put(RerollMasteryDisplay.REROLL_MAP, rerollMap);
        }
        Integer rerollCount = rerollMap.get(spec.getHullId());
        rerollCount = rerollCount == null ? 1 : rerollCount + 1;
        rerollMap.put(spec.getHullId(), rerollCount);
        ModPlugin.setRerolledMasteriesThisSave();

        try {
            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSector().getPlayerPerson(), spec, new MasteryUtils.MasteryAction() {
                        @Override
                        public void perform(MasteryEffect effect) {
                            effect.onDeactivate(Global.getSector().getPlayerPerson());
                        }
                    });
            ShipMastery.generateMasteries(spec);
            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSector().getPlayerPerson(), spec, new MasteryUtils.MasteryAction() {
                        @Override
                        public void perform(MasteryEffect effect) {
                            effect.onActivate(Global.getSector().getPlayerPerson());
                        }
                    });

        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Global.getSoundPlayer().playUISound("ui_char_spent_story_point_technology", 1f, 1f);

        // This may make the player's fleet state invalid, i.e. if changing masteries removed a hangar
        // bay on ships that filled it
        Utils.fixPlayerFleetInconsistencies();
        masteryPanel.forceRefresh(true, false, false, true);
    }
}
