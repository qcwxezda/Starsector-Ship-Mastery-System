package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DPIfOnlyShip extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.DPIfOnlyShip,
                Strings.Descriptions.DPIfOnlyShipNeg,
                true,
                true,
                getHullSpec().getHullName(),
                getIncreaseFor(Global.getSector().getPlayerPerson(), getHullSpec().getHullSize()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        FleetMemberAPI fm = stats.getFleetMember();
        if (fm == null) return;
        String thisHullId = Utils.getRestoredHullSpecId(getHullSpec());
        FleetDataAPI fleetData = fm.getFleetData();
        if (fleetData == null) return;
        int count = 0;
        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            if (thisHullId.equals(Utils.getRestoredHullSpecId(member.getHullSpec()))) {
                count++;
                if (count > 1) return;
            }
        }

        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, getIncreaseFor(stats, hullSize) + 1f);
    }

    public float getIncreaseFor(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        return getIncreaseFor(Utils.getCommanderForFleetMember(stats.getFleetMember()), hullSize);
    }

    public float getIncreaseFor(PersonAPI commander, ShipAPI.HullSize hullSize) {
        float increase = getStrength(commander);
        switch (Utils.hullSizeToInt(hullSize)) {
            case 1: increase *= 0.75f; break;
            case 2: increase *= 0.5f; break;
            case 3: increase *= 0.25f; break;
        }
        return Math.max(increase, -1f);
    }
}
