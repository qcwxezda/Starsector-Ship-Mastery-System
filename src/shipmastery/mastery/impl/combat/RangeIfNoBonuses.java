package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
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
                getIncreaseFor(Global.getSector().getPlayerPerson(), getHullSpec()));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        StatBonus energyStats = ship.getMutableStats().getEnergyWeaponRangeBonus();
        StatBonus ballisticStats = ship.getMutableStats().getBallisticWeaponRangeBonus();
        boolean hasEnergyBonus = checkForBonusIn(energyStats.getFlatBonuses().values(), 0f);
        hasEnergyBonus |= checkForBonusIn(energyStats.getMultBonuses().values(), 1f);
        hasEnergyBonus |= checkForBonusIn(energyStats.getPercentBonuses().values(), 0f);
        boolean hasBallisticBonus = checkForBonusIn(ballisticStats.getFlatBonuses().values(), 0f);
        hasBallisticBonus |= checkForBonusIn(ballisticStats.getMultBonuses().values(), 1f);
        hasBallisticBonus |= checkForBonusIn(ballisticStats.getPercentBonuses().values(), 0f);

        if (!hasEnergyBonus) {
            modify(energyStats, id, getIncreaseFor(ship, ship.getHullSpec()) + 1f);
        }
        if (!hasBallisticBonus) {
            modify(ballisticStats, id, getIncreaseFor(ship, ship.getHullSpec()) + 1f);
        }
    }

    boolean checkForBonusIn(Collection<MutableStat.StatMod> mods, float threshold) {
        for (MutableStat.StatMod mod : mods) {
            if (mod.getSource() != null) {
                if (mod.getSource().startsWith("gunnery_implants")) continue;
                if (mod.getSource().startsWith("ballistic_mastery")) continue;
            }
            if (mod.getValue() > threshold) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.RangeIfNoBonusesPost, 0f);
    }

    public float getIncreaseFor(ShipAPI ship, ShipHullSpecAPI spec) {
        return adjustForHullSize(getStrength(ship), spec.getHullSize());
    }

    public float getIncreaseFor(PersonAPI commander, ShipHullSpecAPI spec) {
        return adjustForHullSize(getStrength(commander), spec.getHullSize());
    }

    public float adjustForHullSize(float amount, ShipAPI.HullSize hullSize) {
        switch (hullSize) {
            case FRIGATE: amount /= 6f; break;
            case DESTROYER: amount /= 3f; break;
            case CRUISER: amount *= 2f / 3f; break;
        }
        return Math.max(amount, -1f);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.isBuiltInMod(HullMods.DEDICATED_TARGETING_CORE) || spec.isBuiltInMod(HullMods.INTEGRATED_TARGETING_UNIT) || spec.isBuiltInMod(HullMods.ADVANCED_TARGETING_CORE)) return null;
        // Works too well with DTC
        if (spec.isBuiltInMod(HullMods.DISTRIBUTED_FIRE_CONTROL)) return 0f;
        return 1.5f;
    }
}
