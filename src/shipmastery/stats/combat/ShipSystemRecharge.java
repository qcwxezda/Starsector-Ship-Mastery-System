package shipmastery.stats.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShipSystemRecharge extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSystemRegenBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getShipSystemId() == null) return null;
        ShipSystemSpecAPI systemSpec = Global.getSettings().getShipSystemSpec(spec.getShipSystemId());
        if (systemSpec == null) return null;
        if (systemSpec.getMaxUses(null) == Integer.MAX_VALUE) return null;

        float regen = systemSpec.getRegen(null);
        if (regen <= 0.0f) return null; // Doesn't regenerate, so would have no effect
        float cooldown = 1f / regen;

        // Ignore extremely long cooldowns
        if (cooldown > 1000f) return null;

        // Otherwise, prefer slower recharging
        return Utils.getSelectionWeightScaledByValueIncreasing(cooldown,
                0f, 8f, 60f);
    }
}
