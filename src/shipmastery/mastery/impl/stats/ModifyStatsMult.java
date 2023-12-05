package shipmastery.mastery.impl.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import shipmastery.stats.ShipStat;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Map;

public class ModifyStatsMult extends ModifyStatsEffect {
    @Override
    float getModifiedAmount(ShipStat stat, float amount) {
        return amount;
    }

    @Override
    String getAmountString(ShipStat stat, float modifiedAmount) {
        return Utils.absValueAsPercent(modifiedAmount);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        for (Map.Entry<ShipStat, Float> entry : amounts.entrySet()) {
            ShipStat stat = entry.getKey();
            float amount = getStrength(stats) * entry.getValue();
            modify(stat.get(stats), id + "_" + stat.getClass().getSimpleName(), amount);
        }
    }
    
    void modify(Object statOrList, String id, float amount) {
        // Negative effects stack multiplicatively
        // Positive effects stack additively
        float mult = Math.max(0f, 1f + amount);
        if (statOrList instanceof StatBonus) {
            if (mult > 1) {
                ((StatBonus) statOrList).modifyPercent(id, (mult - 1f) * 100f, Strings.SHIP_MASTERY_EFFECT);
            }
            else {
                ((StatBonus) statOrList).modifyMult(id, mult);
            }
        }
        else if (statOrList instanceof MutableStat) {
            if (mult > 1) {
                ((MutableStat) statOrList).modifyPercent(id, (mult - 1f) * 100f, Strings.SHIP_MASTERY_EFFECT);
            }
            else {
                ((MutableStat) statOrList).modifyMult(id, mult);
            }
        }
        else {
            for (Object stat : (Object[]) statOrList) {
                modify(stat, id, amount);
            }
        }
    }
}
