package shipmastery.achievements;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import org.magiclib.achievements.MagicAchievement;
import shipmastery.campaign.items.BasePseudocorePlugin;
import shipmastery.util.Utils;

public class PseudocoreCrewedShip extends MagicAchievement {
    @Override
    public void advanceAfterInterval(float amount) {
        if (Global.getSector() == null || Global.getCurrentState() != GameState.CAMPAIGN || Global.getSector().isPaused()) return;
        if (isComplete()) return;

        // It's possible that another mod allows placing AI cores in charge of crewed ships, etc. Want to check for that here.
        for (FleetMemberAPI fm : Utils.getMembersNoSync(Global.getSector().getPlayerFleet())) {
            if (Misc.isAutomated(fm)) continue;
            if (fm.getCaptain() != null && fm.getCaptain().isAICore()) {
                String id = fm.getCaptain().getAICoreId();
                if (id != null) {
                    var spec = Global.getSettings().getCommoditySpec(id);
                    if (spec != null && spec.hasTag(BasePseudocorePlugin.IS_PSEUDOCORE_TAG)) {
                        completeAchievement();
                        return;
                    }
                }
            }
        }
    }
}
