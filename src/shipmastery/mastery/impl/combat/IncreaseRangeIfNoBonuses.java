package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

import java.util.Collection;

public class IncreaseRangeIfNoBonuses extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return makeGenericDescription(
                Strings.INCREASE_RANGE_IF_NO_BONUSES,
                Strings.INCREASE_RANGE_IF_NO_BONUSES_NEG,
                true,
                false,
                getIncreaseFor(spec));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        StatBonus energyStats = ship.getMutableStats().getEnergyWeaponRangeBonus();
        StatBonus ballisticStats = ship.getMutableStats().getBallisticWeaponRangeBonus();
        boolean hasEnergyBonus = checkForBonusIn(energyStats.getFlatBonuses().values(), 0f);
        hasEnergyBonus |= checkForBonusIn(energyStats.getMultBonuses().values(), 1f);
        hasEnergyBonus |= checkForBonusIn(energyStats.getPercentBonuses().values(), 0f);
        boolean hasBallisticBonus = checkForBonusIn(ballisticStats.getFlatBonuses().values(), 0f);
        hasBallisticBonus |= checkForBonusIn(ballisticStats.getMultBonuses().values(), 1f);
        hasBallisticBonus |= checkForBonusIn(ballisticStats.getPercentBonuses().values(), 0f);

        if (!hasEnergyBonus) {
            modify(energyStats, id, getIncreaseFor(ship.getHullSpec()) + 1f);
        }
        if (!hasBallisticBonus) {
            modify(ballisticStats, id, getIncreaseFor(ship.getHullSpec()) + 1f);
        }
    }

    boolean checkForBonusIn(Collection<MutableStat.StatMod> mods, float threshold) {
        for (MutableStat.StatMod mod : mods) {
            if (mod.getValue() > threshold) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.INCREASE_RANGE_IF_NO_BONUSES_POST, 5f);
    }

    public float getIncreaseFor(ShipHullSpecAPI spec) {
        float increase = getIncrease();
        switch (spec.getHullSize()) {
            case FRIGATE:
                return increase / 6f;
            case DESTROYER:
                return 2f*increase/6f;
            case CRUISER:
                return 4f*increase/6f;
            case CAPITAL_SHIP:
                return increase;
            default:
                return 0f;
        }
    }
}
