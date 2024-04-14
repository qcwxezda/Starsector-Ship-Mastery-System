package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PeakCRMultipleShips extends MultiplicativeMasteryEffect {

    static final int MAX_STACKS = 10;
    static final float MAX_INCREASE = 1f;

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
        for (FleetMemberAPI member : Utils.getMembersNoSync(fleetData)) {
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
        tooltip.addPara(Strings.Descriptions.PeakCRMultipleShipsPost, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, "" + MAX_STACKS,
                        Utils.absValueAsPercent(MAX_INCREASE));
    }

    public float getIncreaseFor(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        return adjustForHullSize(getStrength(stats), hullSize);
    }

    public float getIncreaseFor(PersonAPI commander, ShipAPI.HullSize hullSize) {
        return adjustForHullSize(getStrength(commander), hullSize);

    }

    public float adjustForHullSize(float value, ShipAPI.HullSize hullSize) {
        switch (hullSize) {
            case FRIGATE: value *= 0.25f; break;
            case DESTROYER: value *= 0.5f; break;
            case CRUISER: value *= 0.75f; break;
        }
        return value;
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        float dp = spec.getSuppliesToRecover();
        return Utils.getSelectionWeightScaledByValue(dp, 8f, true);
    }
}
