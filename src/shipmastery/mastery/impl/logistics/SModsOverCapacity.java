package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MasteryUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;

public class SModsOverCapacity extends AdditiveMasteryEffect {
    static float DP_PENALTY_PER_SMOD = 0.05f;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        int increase = getIncreasePlayer();
        return MasteryDescription.initDefaultHighlight(
                                         increase == 1 ? Strings.Descriptions.SModsOverCapacitySingle : Strings.Descriptions.SModsOverCapacityPlural)
                                 .params(increase);
    }

    @Override
    public void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.OVER_LIMIT_SMOD_COUNT.modifyFlat(id, getIncreasePlayer());
    }

    @Override
    public void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule) {
        TransientSettings.OVER_LIMIT_SMOD_COUNT.unmodify(id);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats == null || stats.getVariant() == null || stats.getFleetMember() == null) return;

        int overMax = stats.getVariant().getSMods().size() - SModUtils.getMaxSMods(stats);
        if (overMax > 0) {
            // Should only be applied once per ship, not once per mastery effect
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(
                    MasteryUtils.makeSharedId(this),
                    (int) Math.ceil(stats.getFleetMember().getUnmodifiedDeploymentPointsCost() * DP_PENALTY_PER_SMOD *
                                    overMax));
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.SModsOverCapacityPost, 0f, Misc.getNegativeHighlightColor(), "" + (int) (100f * DP_PENALTY_PER_SMOD) + "%");
    }
}
