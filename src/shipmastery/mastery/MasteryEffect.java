package shipmastery.mastery;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

public interface MasteryEffect {
    /** Applied immediately after constructor call.
     *  Effects are constructed once per session, in {@code onApplicationLoad}.
     *  One effect is constructed per hull type per tier, so if two hulls share a mastery effect, they *don't*
     *  also share a {@code MasteryEffect} object.<br><br>
     *
     *  Place {@code args} in {@code mastery_assignments.json} values: <br>
     *    - "effectId" passes no args <br>
     *    - "effectId 1.5" will set the effect strength to 1.5 upon generation <br>
     *    - "effectId 1.5 hello 3" will result in calling {@code init("1.5", "hello", "3")} <br>
     *  If passing args, effect strength must be the first argument.
     *  */
    void init(String... args);

    /** Same usage as {@link HullModEffect#applyEffectsBeforeShipCreation} */
    void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id);

    /** Same usage as {@link HullModEffect#applyEffectsAfterShipCreation} */
    void applyEffectsAfterShipCreation(ShipAPI ship, String id);

    /** Will be displayed in the mastery panel. */
    MasteryDescription getDescription();

    /** Same usage as {@link HullModEffect#applyEffectsToFighterSpawnedByShip} */
    void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id);

    /** Used to check mastery eligibility when randomly selected. */
    boolean isApplicableToHull(ShipHullSpecAPI spec);

    /** Controls how the random selection process is allowed to pick this mastery effect:<br>
     *    - Can only be selected for mastery tiers that are greater than or equal to this method's return value<br>
     *    - Doesn't apply to randomizer mode, where all effects are available at all levels.<br>
     *    - Return {@code null} to prevent random selection even in randomizer mode.
     **/
    Integer getSelectionTier();

    /** Prevents the randomized selection process from picking this effect if it already exists in a different
     *  tier. */
    boolean isUniqueEffect();

    /** Same usage as {@link HullModEffect#advanceInCampaign} */
    void advanceInCampaign(FleetMemberAPI member, float amount);

    /** Same usage as {@link HullModEffect#advanceInCombat} */
    void advanceInCombat(ShipAPI ship, float amount);

    /** Will be displayed in the mastery panel. */
    void addPostDescriptionSection(TooltipMakerAPI tooltip);

    /** Adds a tooltip that shows upon hovering over the effect if {@link MasteryEffect#hasTooltip()}. */
    void addTooltip(TooltipMakerAPI tooltip);
    boolean hasTooltip();

    /** If {@code false}, the description is only shown when the effect can be unlocked. */
    boolean alwaysShowDescription();

    /** All mastery effects have a strength value. Strength is assigned on {@link MasteryEffect#init} as the first
     *  parameter, and defaults to {@code 1} if no parameters are passed. */
    float getStrength();
    void setStrength(float strength);

    /** Changes will be applied when a ship with hull spec {@code spec} is selected inside the refit screen.
     *  Any global changes made should be reverted in {@link MasteryEffect#unapplyEffectsOnEndRefit(ShipHullSpecAPI, String)}.
     *  Effects are applied in ascending order of mastery level. */
    void applyEffectsOnBeginRefit(ShipHullSpecAPI spec, String id);

    /** Will be called when a ship with hull spec {@code spec} is no longer selected inside the refit screen.
     *  Effects are reverted in descending order of mastery level. */
    void unapplyEffectsOnEndRefit(ShipHullSpecAPI spec, String id);

    /** Whether the mastery effect should automatically be activated upon unlocking it. */
    boolean isAutoActivateWhenUnlocked(ShipHullSpecAPI spec);

    /** Whether this mastery effect can be toggled off.
     *  Fine-grained deactivation protocols based on game state are not allowed or possible. To understand why,
     *  consider the following adversarial example: <br>
     *     - Use mastery to allow building in of safety overrides <br>
     *     - Store ship somewhere or sell it <br>
     *     - Deactivate the mastery effect <br>
     *     - Retrieve the ship or buy it back <br>
     *  This shows why we cannot simply detect if a ship in the player's fleet has safety overrides built in and
     *  only allow deactivation if no such ship exists! */
    boolean canBeDeactivated();
}
