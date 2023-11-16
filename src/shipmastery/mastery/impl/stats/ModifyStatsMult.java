package shipmastery.mastery.impl.stats;

import com.fs.starfarer.api.combat.*;
import shipmastery.stats.ShipStat;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.*;

public class ModifyStatsMult extends ModifyStatsEffect {
    @Override
    public void init(String... args) {
        super.init(args);
    }

    @Override
    float getModifiedAmount(ShipStat stat, float amount) {
        return amount;
    }

    @Override
    String getAmountString(ShipStat stat, float modifiedAmount) {
        return Utils.absValueAsPercent(modifiedAmount);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        for (Map.Entry<ShipStat, Float> entry : amounts.entrySet()) {
            ShipStat stat = entry.getKey();
            float amount = getStrength() * entry.getValue();
            float mult = Math.max(0f, 1f + amount);

            Object o = stat.get(stats);
            // Negative effects stack multiplicatively
            // Positive effects stack additively
            if (mult > 1) {
                if (o instanceof StatBonus) {
                    ((StatBonus) o).modifyPercent(id, (mult - 1f) * 100f, Strings.SHIP_MASTERY_EFFECT);
                }
                else {
                    ((MutableStat) o).modifyPercent(id, (mult - 1f) * 100f, Strings.SHIP_MASTERY_EFFECT);
                }
            }
            else {
                if (o instanceof StatBonus) {
                    ((StatBonus) o).modifyMult(id, mult);
                }
                else {
                    ((MutableStat) o).modifyMult(id, mult);
                }
            }
        }
    }
}
