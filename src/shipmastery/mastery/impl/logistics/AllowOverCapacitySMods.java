package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.TransientSettings;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.MasteryUtils;
import shipmastery.util.SModUtils;
import shipmastery.util.Strings;

public class AllowOverCapacitySMods extends AdditiveMasteryEffect {
    static float DP_PENALTY_PER_SMOD = 0.05f;

    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return MasteryDescription.initDefaultHighlight(
                                         getIncrease() == 1 ? Strings.ALLOW_OVER_CAPACITY_SMOD_SINGLE : Strings.ALLOW_OVER_CAPACITY_SMOD_PLURAL)
                                 .params(getIncrease());
    }

    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.OVER_LIMIT_SMOD_COUNT.modifyFlat(id, getIncrease());
    }

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {
        TransientSettings.OVER_LIMIT_SMOD_COUNT.unmodify(id);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
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
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.ALLOW_OVER_CAPACITY_SMOD_POST, 5f, Misc.getNegativeHighlightColor(), "" + (int) (100f * DP_PENALTY_PER_SMOD) + "%");
    }
}
