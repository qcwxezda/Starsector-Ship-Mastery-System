package shipmastery.hullmods.integration;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class IntegratedBetaKCore extends PseudocoreIntegrationHullmod {

    public static final int S_LIMIT = 3;
    public static final float CR_REDUCTION = 0.5f;

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.betaIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asInt(1),
                Utils.asInt(S_LIMIT),
                Utils.asPercent(CR_REDUCTION));
    }

    @Override
    public float getMinIntegrationCost(FleetMemberAPI member) {
        return 75000f;
    }

    @Override
    public float getMaxIntegrationCost(FleetMemberAPI member) {
        return 250000f;
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        var smod = stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD);
        smod.modifyFlat(id, 1);
        var variant = stats.getVariant();
        if (variant == null) return;
        if (variant.getSMods().size() > S_LIMIT) {
            stats.getMaxCombatReadiness().modifyFlat(id, -CR_REDUCTION, Strings.Items.integrationDesc);
        }
    }
}
