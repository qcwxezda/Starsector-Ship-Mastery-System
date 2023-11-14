package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public abstract class BaseMasteryEffect implements MasteryEffect {

    protected float strength = 1f;

    @Override
    public void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id) {}

    @Override
    public void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id) {}

    @Override
    public boolean isAutoActivateWhenUnlocked(ShipHullSpecAPI spec) {
        return true;
    }

    @Override
    public boolean isUniqueEffect() {
        return false;
    }

    @Override
    public boolean canBeDeactivated() {
        return true;
    }

    @Override
    public void init(String... args) {
        if (args == null || args.length == 0) return;

        try {
            setStrength(Float.parseFloat(args[0]));
        } catch (NumberFormatException e) {
            throw new RuntimeException("First argument of mastery initialization must be a number", e);
        }
    }

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
    public Integer getSelectionTier() {
        return 0;
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

    @Override
    public final void setStrength(float strength) {
        this.strength = strength;
    }

    @Override
    public boolean alwaysShowDescription() {
        return false;
    }
}
