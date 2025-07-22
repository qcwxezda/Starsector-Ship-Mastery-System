package shipmastery.achievements;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.util.Utils;

public class AmorphousPseudocoreUsed extends MagicAchievement {
    @Override
    public void advanceAfterInterval(float amount) {
        if (Global.getSector() == null || Global.getCurrentState() != GameState.CAMPAIGN || Global.getSector().isPaused()) return;
        if (isComplete()) return;

        // It's possible that another mod allows placing AI cores in charge of crewed ships, etc. Want to check for that here.
        for (FleetMemberAPI fm : Utils.getMembersNoSync(Global.getSector().getPlayerFleet())) {
            if (canCompleteAchievement(fm)) {
                completeAchievement();
                return;
            }
        }
    }

    public static boolean canCompleteAchievement(FleetMemberAPI fm) {
        if (fm.getCaptain() == null || !fm.getCaptain().isAICore()) return false;
        if (Misc.isAutomated(fm)) return false;
        String id = fm.getCaptain().getAICoreId();
        return "sms_amorphous_pseudocore".equals(id);
    }

}
