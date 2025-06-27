package shipmastery.hullmods.integration;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class IntegratedGammaKCore extends PseudocoreIntegrationHullmod {
    public static final float CR_PER_HULLMOD = 0.02f;
    public static final float CR_INITIAL = 0.1f;

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.gammaIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(CR_INITIAL),
                Utils.asPercent(CR_PER_HULLMOD));
    }

    @Override
    public float getMinIntegrationCost(FleetMemberAPI member) {
        return 50000f;
    }

    @Override
    public float getMaxIntegrationCost(FleetMemberAPI member) {
        return 150000f;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var variant = stats.getVariant();
        if (variant == null) return;
        int count = variant.getNonBuiltInHullmods().size();
        float bonus = CR_INITIAL - CR_PER_HULLMOD * count;
        if (Misc.isAutomated(variant)) bonus *= 2f;
        stats.getMaxCombatReadiness().modifyFlat(id, bonus, Strings.Items.integrationDesc);
    }
}
