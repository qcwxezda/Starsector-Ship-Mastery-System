package shipmastery.util;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;

public abstract class SModUtils {

    public static int[] BASE_VALUE_AMTS = new int[] {10000, 30000, 75000, 250000};
    public static float CREDITS_HARD_CAP = 9999999f;

    public static int ADDITIONAL_MP_PER_SMOD = 1;


    public static int getMPCost(HullModSpecAPI spec, ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();

        // If enhancing built-in, always only costs 1
        if (isHullmodBuiltIn(spec, ship.getVariant())) {
            return 1;
        }

        int nSMods = variant.getSMods().size();
        int cost = 1 + Utils.hullSizeToInt(ship.getHullSize()) + ADDITIONAL_MP_PER_SMOD * nSMods;
        // Doubled MP cost if going over the limit
        if (nSMods >= Misc.getMaxPermanentMods(ship)) {
            cost *= 2;
        }
        return cost;
    }

    public static int getCreditsCost(HullModSpecAPI spec, ShipAPI ship) {
        int hullSizeOrd = Utils.hullSizeToInt(ship.getHullSize());

        float valueFrac = Math.max(1, ship.getHullSpec().getBaseValue() / BASE_VALUE_AMTS[hullSizeOrd]);
        float cost = 10000f
                * (float) Math.max(1f, Math.pow(spec.getCostFor(ship.getHullSize()), 0.4f))
                * (float) Math.pow(valueFrac, 0.8f)
                * (float) Math.pow((hullSizeOrd + 1), 1.1f);
        cost = (float) (Math.ceil(cost/1000f)*1000f);
        // Hard cap at 10 million credits
        return (int) (Math.min(cost, CREDITS_HARD_CAP));
    }

    public static boolean isHullmodBuiltIn(HullModSpecAPI spec, ShipVariantAPI variant) {
        return variant.getHullSpec().isBuiltInMod(spec.getId());
    }
}
