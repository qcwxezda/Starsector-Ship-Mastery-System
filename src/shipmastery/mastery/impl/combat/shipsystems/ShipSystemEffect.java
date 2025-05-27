package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import shipmastery.mastery.BaseMasteryEffect;

public abstract class ShipSystemEffect extends BaseMasteryEffect {
    String name;
    public String getSystemName() {
        if (name != null) return name;
        return name = Global.getSettings().getShipSystemSpec(getSystemSpecId()).getName();
    }

    @Override
    public final void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        applyEffectsAfterShipCreationIfHasSystem(ship);
    }

    @Override
    public final void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || stats.getVariant().getHullSpec() == null || !getSystemSpecId().equals(stats.getVariant().getHullSpec().getShipSystemId())) {
            return;
        }
        applyEffectsBeforeShipCreationIfHasSystem(hullSize, stats);
    }

    @Override
    public final void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        applyEffectsToFighterIfHasSystem(fighter, ship);
    }

    @Override
    public final void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship) {
        if (ship == null || ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) {
            return;
        }
        onFlagshipStatusGainedIfHasSystem(commander, stats, ship);
    }

    public void applyEffectsBeforeShipCreationIfHasSystem(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {}
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {}
    public void applyEffectsToFighterIfHasSystem(ShipAPI fighter, ShipAPI ship) {}
    public void onFlagshipStatusGainedIfHasSystem(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship) {}

    public ShipSystemSpecAPI getSystemSpec() {
        return Global.getSettings().getShipSystemSpec(getSystemSpecId());
    }

    public abstract String getSystemSpecId();

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getShipSystemId() == null || !spec.getShipSystemId().equals(getSystemSpecId())) return null;
        return 3f;
    }
}
