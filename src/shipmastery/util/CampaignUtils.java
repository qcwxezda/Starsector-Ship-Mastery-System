package shipmastery.util;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class CampaignUtils {

    public static PersonAPI getFleetCommanderForStats(MutableShipStatsAPI stats) {
        var lookup = VariantLookup.getVariantInfo(stats.getVariant());
        if (lookup == null) return null;
        return lookup.commander;

//        if (stats == null) {
//            if (BaseSkillEffectDescription.isInCampaign()) {
//                return Global.getSector().getPlayerPerson();
//            }
//            return null;
//        }
//
//        FleetMemberAPI member = stats.getFleetMember();
//        if (member == null) return null;
//        PersonAPI commander = member.getFleetCommanderForStats();
//        if (commander == null) {
//            boolean orig = false;
//            if (member.getFleetData() != null) {
//                orig = member.getFleetData().isForceNoSync();
//                member.getFleetData().setForceNoSync(true);
//            }
//            commander = member.getFleetCommander();
//            if (member.getFleetData() != null) {
//                member.getFleetData().setForceNoSync(orig);
//            }
//        }
//        return commander;
    }

    public static PersonAPI getCaptain(MutableShipStatsAPI stats) {
        PersonAPI captain;
        if (stats.getEntity() instanceof ShipAPI ship) {
            captain = ship.getCaptain();
        } else {
            captain = stats.getFleetMember() == null ? null : stats.getFleetMember().getCaptain();
        }
        return captain;
    }
}
