package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class BomberCost extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getDynamic().getMod(Stats.BOMBER_COST_MOD);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getFighterBays() <= 0) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getFighterBays(), 2f, false);
    }
}
