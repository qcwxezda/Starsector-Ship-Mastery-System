package shipmastery.ui.triggers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.items.KnowledgeConstructPlugin;
import shipmastery.config.Settings;
import shipmastery.ui.MasteryPanel;
import shipmastery.util.IntRef;
import shipmastery.util.Strings;

public class ConfirmCreateConstruct extends DialogDismissedListener{

    final MasteryPanel masteryPanel;
    final ShipHullSpecAPI spec;
    final IntRef count;
    final IntRef spGained;

    public ConfirmCreateConstruct(MasteryPanel masteryPanel, ShipHullSpecAPI spec, IntRef count, IntRef spGained) {
        this.masteryPanel = masteryPanel;
        this.spec = spec;
        this.count = count;
        this.spGained = spGained;
    }

    @Override
    public void trigger(Object... args) {
        // The second argument is 0 if confirmed, 1 if canceled
        int option = (int) args[1];
        if (option == 1) return;

        int amount = KnowledgeConstructPlugin.NUM_POINTS_GAINED;
        ShipMastery.spendPlayerMasteryPoints(spec, amount * count.value);
        Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                count.value == 1 ? Strings.MasteryPanel.createConstructConfirmSingular :
                        String.format(Strings.MasteryPanel.createConstructConfirmPlural, count.value),
                Settings.MASTERY_COLOR);

        Global.getSector().getPersistentData().compute(ConstructButtonPressed.CONSTRUCTS_MADE_KEY, (k,v) -> v == null ? count.value : (int) v + count.value);
        if (spGained.value > 0) {
            Global.getSector().getPlayerStats().addStoryPoints(spGained.value);
            Global.getSector().getCampaignUI().getMessageDisplay().addMessage(
                    String.format(spGained.value == 1 ? Strings.MasteryPanel.createConstructGainedSPSingular : Strings.MasteryPanel.createConstructGainedSPPlural, spGained.value),
                    "" + spGained.value,
                    Misc.getStoryBrightColor()
            );
        }

        Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData("sms_construct", KnowledgeConstructPlugin.PLAYER_CREATED_PREFIX + spec.getHullId()), count.value);
        Global.getSoundPlayer().playUISound("sms_create_construct", 1f, 1f);

        masteryPanel.forceRefresh(true, false, true, false);
    }
}
