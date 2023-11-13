package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public abstract class BaseMasteryEffect implements MasteryEffect {

    float strength = 1f;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {}

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {}

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {}

    @Override
    public boolean isApplicableToHull(ShipHullSpecAPI spec) {
        return true;
    }

    @Override
    public float getRandomizerWeight() {
        return 1;
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {}

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {}

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip) {}

    @Override
    public void addTooltip(TooltipMakerAPI tooltip) {}

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public final float getStrength() {
        return strength;
    }

    public final MasteryEffect setStrength(float strength) {
        this.strength = strength;
        return this;
    }

    @Override
    public boolean hideUntilUnlocked() {
        return false;
    }
}
