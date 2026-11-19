package shipmastery.stats.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Utils;

public class ShipSystemCooldown extends ShipStat {
    @Override
    public Object get(MutableShipStatsAPI stats) {
        return stats.getSystemCooldownBonus();
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getShipSystemId() == null) return null;
        ShipSystemSpecAPI systemSpec = Global.getSettings().getShipSystemSpec(spec.getShipSystemId());
        if (systemSpec == null) return null;
        if (systemSpec.getMaxUses(null) != Integer.MAX_VALUE) return null;

        float cooldown = systemSpec.getCooldown(null);

        // Ignore extremely long cooldowns
        if (cooldown > 1000f) return null;

        // Otherwise, prefer slower recharging
        return Utils.getSelectionWeightScaledByValueIncreasing(cooldown,
                0f, 8f, 60f);
    }
}
