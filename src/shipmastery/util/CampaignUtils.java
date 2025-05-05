package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;

public class CampaignUtils {
    // Adapted from BaseSkillEffectDescription.getCommanderStats
    public static PersonAPI getFleetCommanderForStats(MutableShipStatsAPI stats) {
        if (stats == null) {
            if (BaseSkillEffectDescription.isInCampaign()) {
                return Global.getSector().getPlayerPerson();
            }
            return null;
        }

        FleetMemberAPI member = stats.getFleetMember();
        if (member == null) return null;
        PersonAPI commander = member.getFleetCommanderForStats();
        if (commander == null) {
            boolean orig = false;
            if (member.getFleetData() != null) {
                orig = member.getFleetData().isForceNoSync();
                member.getFleetData().setForceNoSync(true);
            }
            commander = member.getFleetCommander();
            if (member.getFleetData() != null) {
                member.getFleetData().setForceNoSync(orig);
            }
        }
        return commander;
    }
}
