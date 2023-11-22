package shipmastery.mastery;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Note: If a method takes an {@code id} as a parameter, the {@code id} given is
 *  {@code shipmastery_[ID]_[LEVEL]} if {@link MasteryTags#UNIQUE} is not set, i.e. is stackable, and
 *  {@code shipmastery_[ID]} otherwise. <br>
 *  Use {@link shipmastery.util.MasteryUtils#makeSharedId} to get a non-unique {@code id}, useful for effects that have both
 *  unique and stackable elements. <br>
 *  {@code advanceInCombat} not supported, add an {@link com.fs.starfarer.api.combat.listeners.AdvanceableListener} instead.
 *  */
public interface MasteryEffect {
    /** Applied immediately after constructor call.
     *  Effects are constructed once per session, in {@code onApplicationLoad}.
     *  One effect is constructed per hull type per tier, so if two hulls share a mastery effect, they *don't*
     *  also share a {@code MasteryEffect} object.<br><br>
     *
     *  Place {@code args} in {@code mastery_assignments.json} values: <br>
     *    - "effectId" passes no args <br>
     *    - "effectId 0.5" will set the effect strength to 0.5 upon generation <br>
     *    - "effectId 0.5 hello 3" will result in calling {@code init("0.5", "hello", "3")} <br>
     *  If passing args, effect strength must be the first argument.
     *  */
    void init(String... args);

    /** Same usage as {@link HullModEffect#applyEffectsBeforeShipCreation}. <br>
     *  All mastery effects are applied after all other hullmod effects.
     *  Among each other, effects are applied in ascending priority order. */
    void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats);

    /** Same usage as {@link HullModEffect#applyEffectsAfterShipCreation} <br>
     *  All mastery effects are applied after all other hullmod effects.
     *  Among each other, effects are applied in ascending priority order. */
    void applyEffectsAfterShipCreation(ShipAPI ship);

    /** Will be displayed in the mastery panel. */
    MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember);

    /** Same usage as {@link HullModEffect#applyEffectsToFighterSpawnedByShip}  <br>
     *  All mastery effects are applied after all other hullmod effects.
     *  Among each other, effects are applied in ascending priority order. */
    void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship);

    /**
     * The likelihood of the mastery being generated when randomly selected.
     * Return 0 (or less) to indicate that this mastery should not be normally selected.
     * Return {@code null} to indicate that this mastery is not applicable at all and should not be selected
     * even in randomizer mode.
     */
    Float getSelectionWeight();

    /** Will be displayed in the mastery panel. */
    void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember);

    /** Adds a tooltip that shows upon hovering over the effect.
     *  {@link MasteryTags#HAS_TOOLTIP} must be added as a tag in @{code mastery_list.csv}. */
    void addTooltipIfHasTooltipTag(TooltipMakerAPI tooltip, ShipAPI selectedModule, FleetMemberAPI selectedFleetMember);

    /** All mastery effects have a strength value. Strength is assigned on {@link MasteryEffect#init} as the first
     *  parameter, and defaults to {@code default_strength} if no parameters are passed. */
    float getStrength();
    void modifyStrengthMultiplicative(float fraction);
    void modifyStrengthAdditive(float fraction);
    void unmodifyStrength();

    /** Called whenever a ship or module is selected in the refit screen and that ship/module's root ship shares
     *  this mastery's hull spec.
     *  Any global changes made should be reverted in {@link MasteryEffect#onEndRefit(ShipVariantAPI, boolean)}. */
    void onBeginRefit(ShipVariantAPI selectedVariant, boolean isModule);

    /** Called whenever a ship or module is selected in the refit screen and the root ship of the ship/module that was
     *  previously selected shares this mastery's hull spec.
     *  Effects are reverted in descending order of mastery level. */
    void onEndRefit(ShipVariantAPI selectedVariant, boolean isModule);

    /** Called whenever the mastery is activated. Will be called for unique effects even if they are otherwise hidden by a stronger one. */
    void onActivate();

    /** Called whenever the mastery is deactivated. Will be called for unique effects even if they are otherwise hidden by a stronger one. */
    void onDeactivate();
    /** If {@code ship} is null, the game is not actually in combat and {@code stats} should be modified for display purposes only.
     *  The equivalent flagshipStatusLost call does not happen for calls with null {@code ship}.
     *  Note: There may be more than 2 possible values for {@code commander} due to the ability for fleets to merge prior to combat. */
    void onFlagshipStatusGained(PersonAPI commander, MutableShipStatsAPI stats, @Nullable ShipAPI ship);
    /** Note: There may be more than 2 possible values for {@code commander} due to the ability for fleets to merge prior to combat. */
    void onFlagshipStatusLost(PersonAPI commander, MutableShipStatsAPI stats, @NotNull ShipAPI ship);


    /** Affects order of operations when applying multiple mastery effects simultaneously. Default priority is 0. */
    int getPriority();

    void addTags(String... tags);

    @SuppressWarnings("unused")
    void removeTags(String... tags);

    boolean hasTag(String tag);

    /** Hull spec assigned to this mastery effect on generation. Can't be changed. */
    ShipHullSpecAPI getHullSpec();

    /** id assigned to this mastery effect on generation. Can't be changed. */
    String getId();
}
