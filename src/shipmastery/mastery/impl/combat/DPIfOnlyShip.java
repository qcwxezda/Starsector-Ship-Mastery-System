package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.campaign.FleetDataAPI;
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
                getIncreaseFor(getHullSpec().getHullSize()));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        FleetMemberAPI fm = stats.getFleetMember();
        if (fm == null) return;
        String thisHullId = Utils.getRestoredHullSpecId(getHullSpec());
        FleetDataAPI fleetData = fm.getFleetData();
        if (fleetData == null) return;
        int count = 0;
        for (FleetMemberAPI member : fleetData.getMembersListCopy()) {
            if (thisHullId.equals(Utils.getRestoredHullSpecId(getHullSpec()))) {
                count++;
                if (count > 1) return;
            }
        }

        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, getIncreaseFor(hullSize) + 1f);
    }

    public float getIncreaseFor(ShipAPI.HullSize hullSize) {
        float increase = getStrength();
        switch (Utils.hullSizeToInt(hullSize)) {
            case 1: increase *= 0.75f; break;
            case 2: increase *= 0.5f; break;
            case 3: increase *= 0.25f; break;
        }
        return Math.max(increase, -1f);
    }
}
