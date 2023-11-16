package shipmastery.mastery.impl.stats;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.util.Misc;
import shipmastery.stats.StatTags;
import shipmastery.stats.ShipStat;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Map;

public class ModifyStatsFlat extends ModifyStatsEffect {
    @Override
    public void init(String... args) {
        super.init(args);
    }

    @Override
    float getModifiedAmount(ShipStat stat, float amount) {
        if (stat.tags.contains(StatTags.TAG_REQUIRE_INTEGER)) {
            if (amount < 1f && amount > 0f) return 1f;
            if (amount > -1f && amount < 0f) return -1f;
            return (int) amount;
        }
        return amount;
    }

    @Override
    String getAmountString(ShipStat stat, float modifiedAmount) {
        if (stat.tags.contains(StatTags.TAG_DISPLAY_AS_PERCENT)) {
            return Utils.absValueAsPercent(modifiedAmount);
        }
        if (stat.tags.contains(StatTags.TAG_REQUIRE_INTEGER)) {
            return "" + (int) Math.abs(modifiedAmount);
        }
        return Misc.getFormat().format(Math.abs(modifiedAmount));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        for (Map.Entry<ShipStat, Float> entry : amounts.entrySet()) {
            ShipStat stat = entry.getKey();
            float amount = getModifiedAmount(stat, getStrength() * entry.getValue());

            Object o = stat.get(stats);
            if (o instanceof StatBonus) {
                ((StatBonus) o).modifyFlat(id, amount, Strings.SHIP_MASTERY_EFFECT);
            }
            else {
                ((MutableStat) o).modifyFlat(id, amount, Strings.SHIP_MASTERY_EFFECT);
            }
        }
    }
}
