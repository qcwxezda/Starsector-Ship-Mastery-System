package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.mastery.MultiplicativeMasteryEffect;
import shipmastery.util.Strings;

import java.util.Collection;

public class RangeIfNoBonuses extends MultiplicativeMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return makeGenericDescription(
                Strings.Descriptions.RangeIfNoBonuses,
                Strings.Descriptions.RangeIfNoBonusesNeg,
                true,
                false,
                getIncreaseFor(getHullSpec()));
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
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.RangeIfNoBonusesPost, 5f);
    }

    public float getIncreaseFor(ShipHullSpecAPI spec) {
        float increase = getStrength();
        switch (spec.getHullSize()) {
            case FRIGATE: increase /= 6f; break;
            case DESTROYER: increase /= 3f; break;
            case CRUISER: increase *= 2f / 3f; break;
        }
        return Math.max(increase, -1f);
    }
}
