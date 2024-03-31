package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class ConvertedHangarBays extends AdditiveMasteryEffect {
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant().hasHullMod(HullMods.CONVERTED_HANGAR)) {
            stats.getNumFighterBays().modifyFlat(id, getIncrease(stats));
            if (stats.getDynamic().getMod(Stats.CONVERTED_HANGAR_NO_DP_INCREASE).computeEffective(0f) <= 0f) {
                // This doesn't actually do anything because it occurs after converted hangar has already applied the DP increase,
                // but it does enable the green "negated DP increase" text on the hullmod's tooltip
                stats.getDynamic().getMod(Stats.CONVERTED_HANGAR_NO_DP_INCREASE).modifyFlat(id, 1);
                // To actually negate the DP increase, we just remove the modifier with the id converted_hangar
                stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(HullMods.CONVERTED_HANGAR);
                stats.getCRPerDeploymentPercent().unmodify(HullMods.CONVERTED_HANGAR);
                stats.getSuppliesToRecover().unmodify(HullMods.CONVERTED_HANGAR);
            }
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        if (getIncreasePlayer() > 0) {
            tooltip.addPara(
                    Strings.Descriptions.ConvertedHangarBaysPost, 0f, Misc.getTextColor(),
                    Global.getSettings().getHullModSpec(HullMods.CONVERTED_HANGAR).getDisplayName());
        }
    }

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        if (getIncreasePlayer() > 0) {
            return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ConvertedHangarBays)
                                     .params(Global.getSettings().getHullModSpec(HullMods.CONVERTED_HANGAR).getDisplayName(), getIncreasePlayer());
        }
        else {
            return MasteryDescription
                    .initDefaultHighlight(Strings.Descriptions.ConvertedHangarBaysPost)
                    .params(Global.getSettings().getHullModSpec(HullMods.CONVERTED_HANGAR).getDisplayName());
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (ShipAPI.HullSize.FRIGATE.equals(spec.getHullSize())) return null;
        if (spec.getFighterBays() > 0) return null;
        // Don't normally allow this on anything smaller than a cruiser
        if (ShipAPI.HullSize.DESTROYER.equals(spec.getHullSize())) return 0f;
        return 1f;
    }
}
