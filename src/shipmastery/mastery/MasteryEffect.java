package shipmastery.mastery;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public interface MasteryEffect {
    void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id);

    void applyEffectsAfterShipCreation(ShipAPI ship, String id);

    MasteryDescription getDescription();

    void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id);

    /** Used to check mastery eligibility in randomizer mode */
    boolean isApplicableToHull(ShipHullSpecAPI spec);

    /** Used to weigh mastery effects in randomizer mode. Lower is rarer. */
    float getRandomizerWeight();

    void advanceInCampaign(FleetMemberAPI member, float amount);

    void advanceInCombat(ShipAPI ship, float amount);

    void addPostDescriptionSection(TooltipMakerAPI tooltip);
    /** Add a tooltip that shows upon hovering over the effect. */
    void addTooltip(TooltipMakerAPI tooltip);
    boolean hasTooltip();

    /** Whether the effect description is hidden in the menu until unlocked */
    boolean hideUntilUnlocked();

    float getStrength();
}
