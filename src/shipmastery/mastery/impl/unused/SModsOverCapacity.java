package shipmastery.mastery.impl.unused;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MasteryUtils;
import shipmastery.util.HullmodUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Deprecated
public class SModsOverCapacity extends AdditiveMasteryEffect {
    static final float DP_PENALTY_PER_SMOD = 0.05f;
    // Map fleet member id to set of over-max numbers
    public static final Map<String, Set<Integer>> overCapacityMap = new HashMap<>();

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
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

        int overMax = Misc.getCurrSpecialMods(stats.getVariant()) - HullmodUtils.getMaxSMods(stats);
        if (overMax > 0) {
            Set<Integer> ids = overCapacityMap.get(stats.getFleetMember().getId());
            overMax = Math.min(overMax, ids == null ? 0 : ids.size());
            // Should only be applied once per ship, not once per mastery effect
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(
                    MasteryUtils.makeSharedId(this),
                    (int) Math.ceil(stats.getFleetMember().getUnmodifiedDeploymentPointsCost() * DP_PENALTY_PER_SMOD *
                                    overMax));
        }
        else {
            Set<Integer> ids = overCapacityMap.get(stats.getFleetMember().getId());
            if (ids != null) {
                ids.clear();
            }
        }
    }

//    public static void trackOverCapacityMod(FleetMemberAPI fm, int id) {
//        Set<Integer> ids = overCapacityMap.computeIfAbsent(fm.getId(), k -> new HashSet<>());
//        ids.add(id);
//    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // Don't select this
        return null;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipVariantAPI selectedVariant,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.SModsOverCapacityPost, 0f, Settings.NEGATIVE_HIGHLIGHT_COLOR, Utils.asInt((100f * DP_PENALTY_PER_SMOD)) + "%");
    }
}
