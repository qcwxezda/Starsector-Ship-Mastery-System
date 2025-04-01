package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class FighterCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] { stats.getDynamic().getMod(Stats.ALL_FIGHTER_COST_MOD),
                              stats.getDynamic().getMod(Stats.BOMBER_COST_MOD),
                              stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD),
                              stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD),
                              stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD)};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        int modularBays = spec.getFighterBays() - spec.getBuiltInWings().size();
        if (modularBays <= 0) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(modularBays / (1f + Utils.hullSizeToInt(spec.getHullSize())), 0f, 0.5f, 2f);
    }
}
