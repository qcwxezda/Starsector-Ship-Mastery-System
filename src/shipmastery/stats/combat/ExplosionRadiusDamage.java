package shipmastery.stats.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.stats.ShipStat;

public class ExplosionRadiusDamage extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return new Object[] {
                stats.getDynamic().getStat(Stats.EXPLOSION_RADIUS_MULT),
                stats.getDynamic().getStat(Stats.EXPLOSION_DAMAGE_MULT),
                stats.getDynamic().getMod(Stats.EXPLOSION_DAMAGE_MULT),
                stats.getDynamic().getMod(Stats.EXPLOSION_DAMAGE_MULT),};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return spec.isCivilianNonCarrier() ? 1f : 0.5f;
    }
}
