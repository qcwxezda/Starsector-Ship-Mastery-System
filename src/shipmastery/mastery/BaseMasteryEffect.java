package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public abstract class BaseMasteryEffect implements MasteryEffect {

    private float strength = 1f;
    @Override
    public void onBeginRefit(ShipHullSpecAPI spec, String id) {}

    @Override
    public void onEndRefit(ShipHullSpecAPI spec, String id) {}

    @Override
    public void onActivate(ShipHullSpecAPI spec, String id) {}

    @Override
    public void onDeactivate(ShipHullSpecAPI spec, String id) {}

    @Override
    public void init(String... args) {
        if (args == null || args.length == 0) return;

        try {
            strength = Float.parseFloat(args[0]);
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
    public Integer getSelectionTier(ShipHullSpecAPI spec) {
        return 0;
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount, String id) {}

    @Override
    public void advanceInCombat(ShipAPI ship, float amount, String id) {}

    @Override
    public void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {}

    @Override
    public void addTooltip(ShipHullSpecAPI spec, TooltipMakerAPI tooltip) {}

    @Override
    public final float getStrength() {
        return strength;
    }

    @Override
    public final void setStrength(float strength) {
        this.strength = strength;
    }
}
