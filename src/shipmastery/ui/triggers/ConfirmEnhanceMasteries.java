package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.ShipMastery;
import shipmastery.config.Settings;
import shipmastery.ui.EnhanceMasteryDisplay;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.Map;

import static shipmastery.mastery.MasteryEffect.MASTERY_STRENGTH_MOD_FOR;

public class ConfirmEnhanceMasteries extends DialogDismissedListener{

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;

    public ConfirmEnhanceMasteries(MasteryPanel masteryPanel, ShipHullSpecAPI spec) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) return;

        ShipMastery.spendPlayerMasteryPoints(spec, MasteryUtils.getEnhanceMPCost(spec));
        Global.getSector().getPlayerStats().spendStoryPoints(MasteryUtils.getEnhanceSPCost(spec), false, null, false, 0f, null);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                Strings.MasteryPanel.enhanceMasteriesConfirm, Settings.MASTERY_COLOR);

        // Increment enhance count
        //noinspection unchecked
        Map<String, Integer> enhanceMap = (Map<String, Integer>) Global.getSector().getPersistentData().get(EnhanceMasteryDisplay.ENHANCE_MAP);
        if (enhanceMap == null) {
            enhanceMap = new HashMap<>();
            Global.getSector().getPersistentData().put(EnhanceMasteryDisplay.ENHANCE_MAP, enhanceMap);
        }
        Integer enhanceCount = enhanceMap.get(spec.getHullId());
        enhanceCount = enhanceCount == null ? 1 : enhanceCount + 1;
        enhanceMap.put(spec.getHullId(), enhanceCount);

        Global.getSector().getPlayerStats().getDynamic().getMod(MASTERY_STRENGTH_MOD_FOR + spec.getHullId())
                .modifyPercent(EnhanceMasteryDisplay.ENHANCE_MODIFIER_ID, 100f * enhanceCount * Settings.ENHANCE_AMOUNT);
        Global.getSoundPlayer().playUISound("ui_char_spent_story_point_technology", 1f, 1f);

        // This may make the player's fleet state invalid, i.e. if changing masteries removed a hangar
        // bay on ships that filled it
        Utils.fixPlayerFleetInconsistencies();
        masteryPanel.forceRefresh(true, false, true, false);
    }
}
