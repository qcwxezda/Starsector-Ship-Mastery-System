package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ConvertedHangarNoPenalty extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ConvertedHangarNoPenalty).params(
                Global.getSettings().getHullModSpec(HullMods.CONVERTED_HANGAR).getDisplayName());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        // All mastery effects apply after all hullmod effects, so modifying e.g. CONVERTED_HANGAR_NO_CREW_INCREASE
        // has no effect as converted hangar effects get applied first
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).unmodify(HullMods.CONVERTED_HANGAR);
        stats.getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).unmodify(HullMods.CONVERTED_HANGAR);
        stats.getDynamic().getMod(Stats.FIGHTER_REARM_TIME_EXTRA_FRACTION_OF_BASE_REFIT_TIME_MOD).unmodify(HullMods.CONVERTED_HANGAR);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(HullMods.CONVERTED_HANGAR);
        stats.getSuppliesToRecover().unmodify(HullMods.CONVERTED_HANGAR);
        stats.getCRPerDeploymentPercent().unmodify(HullMods.CONVERTED_HANGAR);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (ShipAPI.HullSize.FRIGATE.equals(spec.getHullSize())) return null;
        if (spec.getFighterBays() > 0) return null;
        return 1f;
    }
}
