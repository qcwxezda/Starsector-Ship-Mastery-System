package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetData;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PeakCRMultipleShips extends MultiplicativeMasteryEffect {

    static int MAX_STACKS = 10;
    static float MAX_INCREASE = 1f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.PeakCRMultipleShips,
                Strings.Descriptions.PeakCRMultipleShipsNeg,
                true,
                false,
                getIncreaseFor(Global.getSector().getPlayerPerson(), getHullSpec().getHullSize()),
                getHullSpec().getHullName());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        FleetMemberAPI fm = stats.getFleetMember();
        if (fm == null) return;
        String thisHullId = Utils.getRestoredHullSpecId(getHullSpec());
        FleetDataAPI fleetData = fm.getFleetData();
        if (fleetData == null) return;
        int count = -1;
        for (FleetMemberAPI member : ((FleetData) fleetData).getMembersNoSync()) {
            if (thisHullId.equals(Utils.getRestoredHullSpecId(member.getHullSpec()))) {
                count++;
            }
        }

        if (count > 0) {
            stats.getPeakCRDuration().modifyMult(id, 1f + Math.min(MAX_INCREASE, getIncreaseFor(stats, hullSize) * count));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.PeakCRMultipleShipsPost, 0f, Misc.getHighlightColor(), "" + MAX_STACKS,
                        Utils.absValueAsPercent(MAX_INCREASE));
    }

    public float getIncreaseFor(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        return getIncreaseFor(Utils.getCommanderForFleetMember(stats.getFleetMember()), hullSize);
    }

    public float getIncreaseFor(PersonAPI commander, ShipAPI.HullSize hullSize) {
        float increase = getStrength(commander);
        switch (hullSize) {
            case FRIGATE: increase *= 0.25f; break;
            case DESTROYER: increase *= 0.5f; break;
            case CRUISER: increase *= 0.75f; break;
        }
        return increase;
    }
}
