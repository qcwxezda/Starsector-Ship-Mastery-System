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
        return new Object[] {stats.getSystemCooldownBonus(), stats.getSystemRegenBonus()};
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // No civilian ships
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getShipSystemId() == null) return null;
        ShipSystemSpecAPI systemSpec = Global.getSettings().getShipSystemSpec(spec.getShipSystemId());
        if (systemSpec == null) return null;

        float cooldown;
        if (systemSpec.getMaxUses(null) == Integer.MAX_VALUE) {
            cooldown = systemSpec.getCooldown(null);
        }
        else {
            float regen = systemSpec.getRegen(null);
            if (regen <= 0.0f) return null; // Doesn't regenerate, so would have no effect
            cooldown = 1f / regen;
        }

        // Ignore extremely long cooldowns
        if (cooldown > 1000f) return null;

        // Otherwise, prefer slower recharging
        return Utils.getSelectionWeightScaledByValue(cooldown, 8f, false);
    }
}
