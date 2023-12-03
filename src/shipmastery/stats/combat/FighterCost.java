package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

import java.util.ArrayList;
import java.util.Arrays;

public class FighterCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new ArrayList<>(
                Arrays.asList(stats.getDynamic().getMod(Stats.ALL_FIGHTER_COST_MOD),
                              stats.getDynamic().getMod(Stats.BOMBER_COST_MOD),
                              stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD),
                              stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD),
                              stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD)));
    }

    @Override
    public float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return 0f;
        if (spec.getFighterBays() <= 0) return 0f;
        return 1f;
    }
}
