package shipmastery.mastery;

import com.fs.starfarer.api.ui.TooltipMakerAPI;

public interface MasteryTags {
    /** If set, only the last (in level order) active instance will be used if multiple are simultaneously active.*/
    String UNIQUE = "unique";

    /** If set, can't be disabled once activated.
     *  Fine-grained deactivation protocols based on game state are not allowed or possible. To understand why,
     *  consider the following adversarial example: <br>
     *     - Use mastery to allow building in of safety overrides <br>
     *     - Store ship somewhere or sell it <br>
     *     - Deactivate the mastery effect <br>
     *     - Retrieve the ship or buy it back <br>
     *  This shows why we cannot simply detect if a ship in the player's fleet has safety overrides built in and
     *  only allow deactivation if no such ship exists! */
    String NO_DISABLE = "no_disable";

    /** Set in conjunction with {@link MasteryEffect#addTooltipIfHasTooltipTag(TooltipMakerAPI, com.fs.starfarer.api.combat.ShipAPI, com.fs.starfarer.api.fleet.FleetMemberAPI)} to add a tooltip on mouse hover */
    String HAS_TOOLTIP = "has_tooltip";

    /** If set, will not be automatically activated when unlocked. Should be used for effects that have a downside
     *  and can't be disabled. */
    String NO_AUTO_ACTIVATE = "no_auto_activate";

    /** If set, description will be shown even when not directly unlock-able. */
    String NO_HIDE_DESCRIPTION = "no_hide_description";

    /** Set if effect can double as a debuff by setting strength to a negative value */
    String NEGATIVE_STRENGTH_ALLOWED = "negative_strength_allowed";

    /** Used for random selection purposes */
    String LOGISTIC = "logistic";

    /** Effects with this tag won't ever be selected on civilian ships */
    String COMBAT = "combat";

    /** Effects with this tag won't propagate to modules */
    String DOESNT_AFFECT_MODULES = "doesnt_affect_modules";

    /** Appends "Flagship only: " to the start of the effect description. Actually making the effect flagship only is implementation-dependent. */
    String PREFIX_FLAGSHIP_ONLY = "prefix_flagship_only";

    /** If NPCs with this mastery need to be re-inflated due to increase hangar bays, changed OP costs, etc. */
    String TRIGGERS_AUTOFIT = "triggers_autofit";

    /** Allows the same mastery to be randomly selected as both options at the same level, e.g. if different instances of this
     *  mastery effect can have different effects due to taking additional params. */
    String VARYING = "varying";

    /** Normally, when random masteries are selected at a high tier, high-tier effects are preferred. This tag allows normally low-tier effects
     *  to be selected with a weight as if they were high tier. */
    String SCALE_SELECTION_WEIGHT = "scale_selection_weight";
}
