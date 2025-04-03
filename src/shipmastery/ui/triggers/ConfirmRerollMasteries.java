package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfirmRerollMasteries extends DialogDismissedListener{

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;
    final boolean isNoEffect;

    public ConfirmRerollMasteries(MasteryPanel masteryPanel, ShipHullSpecAPI spec, boolean isNoEffect) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
        this.isNoEffect = isNoEffect;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) return;
        // Needed because spacebar confirms even if the confirm button is grayed out
        if (isNoEffect) return;

        Set<Integer> levels = new HashSet<>();
        for (int i = 1; i <= ShipMastery.getMaxMasteryLevel(spec); i++) {
            levels.add(i);
        }
        levels.removeAll(ShipMastery.getPlayerActiveMasteriesCopy(spec).keySet());


        ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getRerollMPCost(spec));
        Global.getSector().getPlayerStats().spendStoryPoints(MasteryUtils.getRerollSPCost(spec), false, null, false, 0f, null);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                Strings.MasteryPanel.rerollConfirm, Settings.MASTERY_COLOR);

        // Increment reroll count
        //noinspection unchecked
        Map<String, List<Set<Integer>>> rerollMap = (Map<String, List<Set<Integer>>>) Global.getSector().getPersistentData().get(ShipMastery.REROLL_SEQUENCE_MAP);
        if (rerollMap == null) {
            rerollMap = new HashMap<>();
            Global.getSector().getPersistentData().put(ShipMastery.REROLL_SEQUENCE_MAP, rerollMap);
        }
        List<Set<Integer>> rerollSequence = rerollMap.computeIfAbsent(spec.getHullId(), k -> new LinkedList<>());

        rerollSequence.add(levels);
        ShipMastery.addRerolledSpecThisSave(spec);

        try {
            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSector().getPlayerPerson(), spec, effect -> effect.onDeactivate(Global.getSector().getPlayerPerson()));
            ShipMastery.generateMasteries(spec, levels, rerollSequence.size(), true);
            MasteryUtils.applyAllActiveMasteryEffects(
                    Global.getSector().getPlayerPerson(), spec, effect -> effect.onActivate(Global.getSector().getPlayerPerson()));

        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        Global.getSoundPlayer().playUISound("ui_char_spent_story_point_technology", 1f, 1f);

        // This may make the player's fleet state invalid, i.e. if changing masteries removed a hangar
        // bay on ships that filled it
        Utils.fixPlayerFleetInconsistencies();
        masteryPanel.forceRefresh(true, false, true, false);
    }
}
