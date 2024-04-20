package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;

public abstract class SModUtils {

    public static final int[] BASE_VALUE_AMTS = new int[] {10000, 30000, 75000, 250000};
    public static final float CREDITS_HARD_CAP = 9999999f;
    public static final int MP_HARD_CAP = 99;

    public static final int ADDITIONAL_MP_PER_SMOD = 0;
    public static final float DP_PER_EXTRA_MP = 25f;


    public static int getMPCost(HullModSpecAPI spec, ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();
        // Built-in mods always have static cost
        if (isHullmodBuiltIn(spec, ship.getVariant())) {
            return 1;
        }

        int nSMods = variant.getSMods().size();
        ShipHullSpecAPI hullSpec = ship.getHullSpec();
        float dp = hullSpec == null ? 0f : hullSpec.getSuppliesToRecover();
        float cost = 1 + (int) (dp / DP_PER_EXTRA_MP);

        cost += ADDITIONAL_MP_PER_SMOD * nSMods;
        // Exponentially increasing MP cost for each S-mod over the limit
        if (TransientSettings.OVER_LIMIT_SMOD_COUNT.getModifiedInt() >= 1) {
            for (int i = Misc.getMaxPermanentMods(ship); i <= nSMods; i++) {
                cost *= 1.25f;
                // Hard cap here to avoid overflow
                cost = Math.min(cost, MP_HARD_CAP);
            }
        }
        cost -= TransientSettings.SMOD_MP_COST_FLAT_REDUCTION.getModifiedInt();
        cost = Math.max(1, cost);
        return (int) Math.min(cost, MP_HARD_CAP);
    }

    public static int getCreditsCost(HullModSpecAPI spec, ShipAPI ship) {
        int hullSizeOrd = Utils.hullSizeToInt(ship.getHullSize());

        float valueFrac = Math.max(1, ship.getHullSpec().getBaseValue() / BASE_VALUE_AMTS[hullSizeOrd]);
        float cost = 10000f
                * (float) Math.pow(Math.max(1f, spec.getCostFor(ship.getHullSize())), 0.4f)
                * (float) Math.pow(valueFrac, 0.8f)
                * (float) Math.pow((hullSizeOrd + 1), 1.1f);
        cost = (float) (Math.ceil(cost/1000f)*1000f);

        // If enhancing built-in, credits cost is halved
        if (isHullmodBuiltIn(spec, ship.getVariant())) {
            cost /= 2f;
        }

        cost *= TransientSettings.SMOD_CREDITS_COST_MULT.getModifiedValue();
        cost *= Settings.BUILD_IN_CREDITS_COST_MULTIPLIER;

        // Hard cap at 10 million credits
        return (int) (Math.min(cost, CREDITS_HARD_CAP));
    }

    public static boolean isHullmodBuiltIn(HullModSpecAPI spec, ShipVariantAPI variant) {
        return variant.getHullSpec().isBuiltInMod(spec.getId());
    }

    public static int getMaxSMods(FleetMemberAPI fleetMember) {
        return getMaxSMods(fleetMember.getStats());
    }

    public static int getMaxSMods(MutableShipStatsAPI stats) {
        return (int) stats.getDynamic()
                          .getMod(Stats.MAX_PERMANENT_HULLMODS_MOD)
                          .computeEffective(Global.getSettings().getInt("maxPermanentHullmods"));
    }
}
