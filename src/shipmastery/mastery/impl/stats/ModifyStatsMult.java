package shipmastery.mastery.impl.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.stats.ShipStat;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;
import java.util.Map;

public class ModifyStatsMult extends ModifyStatsEffect {
    @Override
    protected float getModifiedAmount(ShipStat stat, float amount) {
        return Math.max(-1f, amount);
    }

    @Override
    protected String getAmountString(ShipStat stat, float modifiedAmount) {
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
                ((StatBonus) statOrList).modifyPercent(id, (mult - 1f) * 100f, Strings.Misc.shipMasteryEffect);
            }
            else {
                ((StatBonus) statOrList).modifyMult(id, mult);
            }
        }
        else if (statOrList instanceof MutableStat) {
            if (mult > 1) {
                ((MutableStat) statOrList).modifyPercent(id, (mult - 1f) * 100f, Strings.Misc.shipMasteryEffect);
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

    @Override
    public List<String> generateRandomArgs(ShipHullSpecAPI spec, int maxTier, long seed) {
        return super.generateRandomArgs(spec, maxTier, seed, false);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        // Select ModifyStatsEffect instead to get better spread between flat and mult stats
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 2f;
    }
}
