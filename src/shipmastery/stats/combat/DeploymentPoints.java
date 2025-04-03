package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class DeploymentPoints extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // Don't select this normally, it's really just for threat ships
        return 0f;
    }
}
