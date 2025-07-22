package shipmastery.achievements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.campaign.FleetHandler;

import java.util.stream.Collectors;

public class WinBattleHighMastery extends MagicAchievement {

    public static final float REQ_AVG_LEVEL = 6f;

    private class Listener extends BaseCampaignEventListener {
        public Listener() {
            super(false);
        }

        @Override
        public void reportPlayerEngagement(EngagementResultAPI result) {
            if (!result.didPlayerWin()) return;
            var commander = result.getLoserResult().getFleet().getCommander();
            // Shouldn't be possible, but maybe some weird mod adds player vs player fights idk
            if (commander == null || commander.isPlayer()) return;

            // No pursuit
            if (result.getLoserResult() == null || result.getLoserResult().getAllEverDeployedCopy() == null) return;
            double averageLevel = result.getLoserResult().getAllEverDeployedCopy().stream().collect(Collectors.averagingInt(dfm -> {
                var map = FleetHandler.getCachedNPCMasteries(commander, dfm.getMember().getHullSpec());
                return map == null || map.isEmpty() ? 0 : map.lastKey();
            }));

            if (averageLevel < REQ_AVG_LEVEL) return;
            completeAchievement();
        }
    }

    @Override
    public void onSaveGameLoaded(boolean isComplete) {
        if (isComplete) return;
        Global.getSector().addTransientListener(new Listener());
    }
}
