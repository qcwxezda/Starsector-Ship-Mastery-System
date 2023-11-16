package shipmastery.mastery;

import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.Set;

/** Note: If a method takes an {@code id} as a parameter, the {@code id} given is
 *  {@code shipmastery_[ID]_[LEVEL]} if {@link MasteryTags#TAG_UNIQUE} is not set, i.e. is stackable, and
 *  {@code shipmastery_[ID]} otherwise. <br>
 *  Use {@link shipmastery.util.MasteryUtils#makeSharedId} to get a non-unique {@code id}, useful for effects that have both
 *  unique and stackable elements.
 *  */
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
    MasteryDescription getDescription(ShipHullSpecAPI spec);

    /** Same usage as {@link HullModEffect#applyEffectsToFighterSpawnedByShip} */
    void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id);

    /** Used to check mastery eligibility when randomly selected. */
    boolean isApplicableToHull(ShipHullSpecAPI spec);

    /** Same usage as {@link HullModEffect#advanceInCampaign} */
    void advanceInCampaign(FleetMemberAPI member, float amount, String id);

    /** Same usage as {@link HullModEffect#advanceInCombat} */
    void advanceInCombat(ShipAPI ship, float amount, String id);

    /** Will be displayed in the mastery panel. */
    void addPostDescriptionSection(ShipHullSpecAPI spec, TooltipMakerAPI tooltip);

    /** Adds a tooltip that shows upon hovering over the effect.
     *  {@link MasteryTags#TAG_HAS_TOOLTIP} must be added as a tag in @{code mastery_list.csv}. */
    void addTooltip(ShipHullSpecAPI spec, TooltipMakerAPI tooltip);

    /** All mastery effects have a strength value. Strength is assigned on {@link MasteryEffect#init} as the first
     *  parameter, and defaults to {@code 1} if no parameters are passed. */
    float getStrength();

    void setStrength(float strength);

    /** Changes will be applied when a ship with hull spec {@code spec} is selected inside the refit screen.
     *  Any global changes made should be reverted, either in {@link MasteryEffect#onEndRefit(ShipHullSpecAPI, String)}
     *  or inside a {@link shipmastery.campaign.listeners.RefitScreenShipChangedListener}.
     *  Effects are applied in ascending order of mastery level. */
    void onBeginRefit(ShipHullSpecAPI spec, String id);

    /** Will be called when a ship with hull spec {@code spec} is no longer selected inside the refit screen.
     *  Effects are reverted in descending order of mastery level. */
    void onEndRefit(ShipHullSpecAPI spec, String id);

    /** Called whenever the mastery is activated. Will be called for unique effects even if they are otherwise hidden by a stronger one. */
    void onActivate(ShipHullSpecAPI spec, String id);

    /** Called whenever the mastery is deactivated. Will be called for unique effects even if they are otherwise hidden by a stronger one. */
    void onDeactivate(ShipHullSpecAPI spec, String id);

    Set<String> getTags();

    void addTags(String... tags);

    void removeTags(String... tags);

    boolean hasTag(String tag);

    int getTier();

    void setTier(int tier);

    float getWeight();

    void setWeight(float weight);
}
