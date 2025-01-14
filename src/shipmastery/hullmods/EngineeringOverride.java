package shipmastery.hullmods;

import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class EngineeringOverride extends BaseLogisticsHullMod {

    public static final int NUM_ADDITIONAL_SMODS = 2;
    public static final float CREDITS_COST_MULT = 0.25f;
    public static final float CR_PENALTY = 0.05f;
    public static final Color NAME_COLOR = Utils.mixColor(Misc.getGrayColor(), Color.WHITE, 0.25f);

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0: return "" + NUM_ADDITIONAL_SMODS;
            case 1: return Utils.asPercent(1f - CREDITS_COST_MULT);
            case 2: return Utils.asPercent(CR_PENALTY);
        }
        return null;
    }

    @Override
    public Color getNameColor() {
        return NAME_COLOR;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        if (ship != null && ship.getVariant() != null && ship.getVariant().getPermaMods().contains(Strings.Hullmods.ENGINEERING_OVERRIDE)) {
            tooltip.addPara(Strings.Hullmods.engineeringOverridePermanent, 8f);
        }
        else {
            tooltip.addPara(Strings.Hullmods.engineeringOverrideWarning, 8f);
        }
    }

    @Override
    public boolean canBeAddedOrRemovedNow(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
        if (ship.getVariant() == null || ship.getVariant().getPermaMods().contains(spec.getId())) return false;
        return super.canBeAddedOrRemovedNow(ship, marketOrNull, mode);
    }

    @Override
    public String getCanNotBeInstalledNowReason(ShipAPI ship, MarketAPI marketOrNull, CampaignUIAPI.CoreUITradeMode mode) {
        // Don't show the "can't be removed without spaceport" text if it's permanent
        if (ship.getVariant() != null && ship.getVariant().getPermaMods().contains(spec.getId())) return null;
        return super.getCanNotBeInstalledNowReason(ship, marketOrNull, mode);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (Settings.DISABLE_MAIN_FEATURES) return;

        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, NUM_ADDITIONAL_SMODS);
        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;
        int sMods = variant.getSMods().size();
        if (sMods > 0) {
            stats.getMaxCombatReadiness().modifyFlat(id, -CR_PENALTY*sMods, Strings.Hullmods.engineeringOverrideDesc);
        }
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        if (ship.getVariant() == null) return false;
        if (ship.getVariant().getPermaMods().contains(Strings.Hullmods.ENGINEERING_OVERRIDE)) {
            // RefitHandler un-hides this spec whenever a new ship is selected
            spec.setHidden(true);
            return false;
        }
        return !Settings.DISABLE_MAIN_FEATURES;
    }
}
