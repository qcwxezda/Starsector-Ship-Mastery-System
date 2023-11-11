package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public abstract class SModUtils {

    public static int[] BASE_VALUE_AMTS = new int[] {10000, 30000, 75000, 250000};
    public static float CREDITS_HARD_CAP = 9999999f;

    public static int ADDITIONAL_MP_PER_SMOD = 1;


    public static int getMPCost(HullModSpecAPI spec, ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();

        // If enhancing built-in, always only costs 1
        if (variant.getHullSpec().getBuiltInMods().contains(spec.getId())) {
            return 1;
        }

        int nSMods = variant.getSMods().size();
        int cost = 1 + ADDITIONAL_MP_PER_SMOD * nSMods;
        // Doubled MP cost if going over the limit
        if (nSMods >= getSModLimit(ship)) {
            cost *= 2;
        }
        return cost;
    }

    public static int getCreditsCost(HullModSpecAPI spec, ShipAPI ship) {
        ShipVariantAPI variant = ship.getVariant();
        int hullSizeOrd = 0;
        switch (variant.getHullSize()) {
            case DESTROYER:
                hullSizeOrd = 1;
                break;
            case CRUISER:
                hullSizeOrd = 2;
                break;
            case CAPITAL_SHIP:
                hullSizeOrd = 3;
                break;
        }

        float valueFrac = Math.max(1, variant.getHullSpec().getBaseValue() / BASE_VALUE_AMTS[hullSizeOrd]);
        float cost = 10000f
                * (float) Math.max(1f, Math.pow(spec.getCostFor(variant.getHullSize()), 0.4f))
                * (float) Math.pow(valueFrac, 0.8f)
                * (float) Math.pow((hullSizeOrd + 1), 0.9f);
        cost = (float) (Math.ceil(cost/1000f)*1000f);
        // Hard cap at 10 million credits
        return (int) (Math.min(cost, CREDITS_HARD_CAP));
    }

    public static int getSModLimit(ShipAPI ship) {
        return (int) ship.getMutableStats().getDynamic()
                .getMod(Stats.MAX_PERMANENT_HULLMODS_MOD)
                .computeEffective(Global.getSettings().getInt("maxPermanentHullmods"));
    }
}
