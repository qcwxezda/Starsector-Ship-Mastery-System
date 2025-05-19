package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Objects;

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
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.DPIfOnlyShipPost, 0f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        FleetMemberAPI fm = stats.getFleetMember();
        if (fm == null) return;
        String thisHullId = Utils.getRestoredHullSpecId(getHullSpec());
        FleetDataAPI fleetData = fm.getFleetData();
        if (fleetData == null) return;
        int count = 0;
        for (FleetMemberAPI member : Utils.getMembersNoSync(fleetData)) {
            if (thisHullId.equals(Utils.getRestoredHullSpecId(member.getHullSpec()))) {
                count++;
                if (count > 1) return;
            }
        }

        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, getIncreaseFor(stats, hullSize) + 1f);
    }

    public float getAdjustedStrength(float strength) {
        float baseStrength = getStrength((PersonAPI) null);
        return (strength + baseStrength)/2f;
    }

    public float getIncreaseFor(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize) {
        return adjustForHullSize(getAdjustedStrength(getStrength(stats)), hullSize);
    }

    public float getIncreaseFor(PersonAPI commander, ShipAPI.HullSize hullSize) {
        return adjustForHullSize(getAdjustedStrength(getStrength(commander)), hullSize);
    }

    public float adjustForHullSize(float amount, ShipAPI.HullSize hullSize) {
        switch (Utils.hullSizeToInt(hullSize)) {
            case 1: amount *= 0.75f; break;
            case 2: amount *= 0.5f; break;
            case 3: amount *= 0.5f; break;
        }
        return Math.max(amount, -1f);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        float dp = spec.getSuppliesToRecover();
        if (dp < 10f) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(dp, 10f, 20f, 60f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        var specId = Utils.getRestoredHullSpecId(fm.getHullSpec());
        var members = Utils.getMembersNoSync(fm.getFleetData());

        int count = (int) members.stream()
                .filter(x -> Objects.equals(specId, Utils.getRestoredHullSpecId(x.getHullSpec())))
                .count();

        return count > 1 ? 0f : 2f*super.getNPCWeight(fm);
    }
}
