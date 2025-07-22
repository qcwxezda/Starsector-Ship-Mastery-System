package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.HullModEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.backgrounds.BackgroundUtils;
import shipmastery.backgrounds.HullTinkerer;
import shipmastery.config.Settings;
import shipmastery.config.TransientSettings;
import shipmastery.hullmods.EngineeringOverride;

import java.util.ArrayList;
import java.util.List;

public abstract class HullmodUtils {

    public static final int[] BASE_VALUE_AMTS = new int[] {10000, 30000, 75000, 250000};
    public static final float CREDITS_HARD_CAP = 9999999f;
    public static final float CREDITS_COST_MULT_SP = 0.1f;
    public static final float CREDITS_COST_BXP_CAP = 200000f;
    public static final float SELECTIVE_RESTORE_COST_MULT_MIN = 0.2f;
    public static final float SELECTIVE_RESTORE_COST_MULT_MAX = 1f;
    public static final float SMOD_REMOVAL_COST_MULT = 0.5f;

    public static int getBuildInCost(HullModSpecAPI spec, ShipAPI ship) {
        return getBuildInCost(spec, ship, false);
    }

    public static int getBuildInCost(HullModSpecAPI spec, ShipAPI ship, boolean usingSP) {
        ShipVariantAPI variant = ship.getVariant();
        if (variant == null) return 0;
        int hullSizeOrd = Utils.hullSizeToInt(ship.getHullSize());

        float valueFrac = Math.max(1, ship.getHullSpec().getBaseValue() / BASE_VALUE_AMTS[hullSizeOrd]);
        float opCost = spec.getCostFor(ship.getHullSize());
        if (spec.getEffect() != null && spec.getEffect().hasSModEffect()) {
            if (spec.getEffect().isSModEffectAPenalty()) {
                opCost *= 0.75f;
            } else {
                opCost = opCost * 1.25f + 10f;
            }
        }

        float cost = 3333f
                * (float) Math.pow(Math.max(2.5f*(hullSizeOrd + 1), opCost), 0.5f)
                * (float) Math.pow(valueFrac, 1.33f)
                * (float) Math.pow((hullSizeOrd + 1), 1.3f);
        cost = (float) (Math.ceil(cost/1000f)*1000f);

        // If enhancing built-in, credits cost is halved
        if (isHullmodBuiltIn(spec, variant)) {
            cost /= 2f;
        }

        // Tinkerer background
        if (BackgroundUtils.isTinkererStart()) {
            cost *= 1f - HullTinkerer.COST_REDUCTION;
        }

        // Engineering override reduces by additional factor
        if (variant.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE)) {
            cost *= EngineeringOverride.CREDITS_COST_MULT;
        }

        // Using SP reduces by additional factor
        if (usingSP) {
            cost *= CREDITS_COST_MULT_SP;
        }

        cost *= TransientSettings.SMOD_CREDITS_COST_MULT.getModifiedValue();
        cost *= Settings.BUILD_IN_CREDITS_COST_MULTIPLIER;

        // Hard cap at 10 million credits
        return (int) (Math.min(cost, CREDITS_HARD_CAP));
    }

    public static boolean hasLogisticSMod(ShipVariantAPI variant) {
        for (String sMod : variant.getSMods()) {
            HullModSpecAPI hullModSpec = Global.getSettings().getHullModSpec(sMod);
            if (hullModSpec.hasUITag(HullMods.TAG_UI_LOGISTICS)) {
                return true;
            }
        }
        return false;
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
                          .computeEffective(Misc.MAX_PERMA_MODS);
    }

    public static boolean hasBonusLogisticSlot(ShipVariantAPI variant) {
        VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(variant);
        ShipVariantAPI rootVariant = info == null || info.root == null ? variant : info.root;
        return !variant.hasHullMod(Strings.Hullmods.ENGINEERING_OVERRIDE)
                && MasteryUtils.getEnhanceCount(rootVariant.getHullSpec()) >= MasteryUtils.bonusLogisticSlotEnhanceNumber;
    }

    public static class HullmodTooltipCreator implements TooltipMakerAPI.TooltipCreator {
        private final HullModSpecAPI hullmod;
        private final ShipAPI ship;

        public HullmodTooltipCreator(HullModSpecAPI hullmod, ShipAPI ship) {
            this.hullmod = hullmod;
            this.ship = ship;
        }

        @Override
        public boolean isTooltipExpandable(Object tooltipParam) {
            return false;
        }

        @Override
        public float getTooltipWidth(Object tooltipParam) {
            return 500f;
        }

        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            HullModEffect effect = hullmod.getEffect();
            ShipAPI.HullSize hullSize = ship.getHullSize();
            tooltip.addTitle(hullmod.getDisplayName());
            tooltip.addSpacer(10f);
            if (effect.shouldAddDescriptionToTooltip(hullSize, ship, false)) {
                List<String> highlights = new ArrayList<>();
                String descParam;
                // hard cap at 100 just in case getDescriptionParam for some reason
                // doesn't default to null
                for (int i = 0; i < 100 && (descParam = effect.getDescriptionParam(i, hullSize, ship)) != null;
                     i++) {
                    highlights.add(descParam);
                }
                tooltip.addPara(hullmod.getDescription(hullSize).replaceAll("%", "%%"), 0f, Misc.getHighlightColor(),
                        highlights.toArray(new String[0]));
            }
            effect.addPostDescriptionSection(tooltip, hullSize, ship, getTooltipWidth(tooltipParam), true);
            if (effect.hasSModEffectSection(hullSize, ship, false)) {
                effect.addSModSection(tooltip, hullSize, ship, getTooltipWidth(tooltipParam), false, true);
            }
        }
    }

    public static float getRestorationCost(ShipVariantAPI variant) {
        if (!variant.isDHull()) {
            return 0;
        } else {
            var spec = variant.getHullSpec();
            var base = spec.getBaseHull();
            if (base == null) {
                base = spec;
            }

            float baseCostMult = Global.getSettings().getFloat("baseRestoreCostMult");
            float costPerMult = Global.getSettings().getFloat("baseRestoreCostMultPerDMod");
            int count = DModManager.getNumDMods(variant);

            return Math.max(spec.getBaseValue(), base.getBaseValue()) * baseCostMult * (float) Math.pow(costPerMult, count);
        }
    }

    public static float getBonusXPFraction(float cost) {
        return Math.min(1f, Math.max(0f, (CREDITS_COST_BXP_CAP - cost) / CREDITS_COST_BXP_CAP));
    }
}
