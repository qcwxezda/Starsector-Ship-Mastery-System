package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.campaign.fleet.FleetData;
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
        for (FleetMemberAPI member : ((FleetData) fleetData).getMembersNoSync()) {
            if (thisHullId.equals(Utils.getRestoredHullSpecId(member.getHullSpec()))) {
                count++;
                if (count > 1) return;
            }
        }

        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, getIncreaseFor(stats, hullSize) + 1f);
    }

    public float getIncreaseFor(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        return adjustForHullSize(getStrength(stats), hullSize);
    }

    public float getIncreaseFor(PersonAPI commander, ShipAPI.HullSize hullSize) {
        return adjustForHullSize(getStrength(commander), hullSize);
    }

    public float adjustForHullSize(float amount, ShipAPI.HullSize hullSize) {
        switch (Utils.hullSizeToInt(hullSize)) {
            case 1: amount *= 0.75f; break;
            case 2: amount *= 0.5f; break;
            case 3: amount *= 0.25f; break;
        }
        return Math.max(amount, -1f);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        float dp = spec.getSuppliesToRecover();
        if (dp < 10f) return null;
        return Utils.getSelectionWeightScaledByValue(dp, 15f, false);
    }
}
