package shipmastery.config;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import shipmastery.mastery.MasteryEffect;

import java.util.HashSet;
import java.util.Set;

/** Settings here generally only apply to one ship hull spec at a time
 *  and must be changed when needed via e.g. {@link MasteryEffect#onBeginRefit}.
 *  Stacks with coincident settings in global {@link Settings}. The stacking is multiplicative.
 *  */
public abstract class TransientSettings {
    public static boolean SMOD_REMOVAL_ENABLED = false;
    public static final Set<String> IGNORE_NO_BUILD_IN_HULLMOD_IDS = new HashSet<>();
    public static final MutableStat OVER_LIMIT_SMOD_COUNT = new MutableStat(0);
    public static final MutableStat SMOD_CREDITS_COST_MULT = new MutableStat(1f);
    public static final MutableStat SMOD_MP_COST_FLAT_REDUCTION = new MutableStat(0);
    public static final MutableStat SHIP_RESTORE_COST_MULT = new MutableStat(Global.getSettings().getFloat("baseRestoreCostMult"));
}
