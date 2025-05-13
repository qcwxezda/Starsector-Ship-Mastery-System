package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Objects;

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
        tooltip.addPara(Strings.Descriptions.PeakCRMultipleShipsPost, 0f, Misc.getTextColor(), "" + MAX_STACKS,
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
            case FRIGATE: value *= 0.5f; break;
            case DESTROYER: value *= 0.5f; break;
        }
        return value;
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (ShipAPI.HullSize.CAPITAL_SHIP.equals(spec.getHullSize())) return 0f;
        if (spec.getSuppliesToRecover() >= 40f) return 0f;
        float dp = spec.getSuppliesToRecover();
        return Utils.getSelectionWeightScaledByValueDecreasing(dp, 3f, 8f, 40f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        var specId = Utils.getRestoredHullSpecId(fm.getHullSpec());
        var members = Utils.getMembersNoSync(fm.getFleetData());

        int count = (int) members.stream()
                .filter(x -> Objects.equals(specId, Utils.getRestoredHullSpecId(x.getHullSpec())))
                .count();

        if (count <= 1) return 0f;
        return count <= 3 ? 0.5f * super.getNPCWeight(fm) : super.getNPCWeight(fm);
    }
}
