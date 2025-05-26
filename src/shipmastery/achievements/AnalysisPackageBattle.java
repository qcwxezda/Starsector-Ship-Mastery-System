package shipmastery.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.campaign.PlayerMPHandler;

public class AnalysisPackageBattle extends MagicAchievement {

    // Anything >= 49.5% gets rounded to 50% in hullmod tooltip
    public static final float BONUS_NEEDED = 0.495f;

    private class Listener extends BaseCampaignEventListener {
        public Listener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            if (!result.didPlayerWin()) return;
            // No pursuit
            if (result.getLoserResult() == null || result.getLoserResult().getAllEverDeployedCopy() == null) return;

            float bonus = Global.getSector().getPlayerStats()
                    .getDynamic()
                    .getStat(PlayerMPHandler.COMBAT_MP_GAIN_STAT_MULT_KEY)
                    .getModifiedValue() - 1f;
            if (bonus < BONUS_NEEDED) return;
            completeAchievement();
        }
    }

    @Override
    public void onSaveGameLoaded(boolean isComplete) {
        if (isComplete) return;
        Global.getSector().addTransientListener(new Listener());
    }
}
