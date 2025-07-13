package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class BetaCoreInterface extends AICoreInterfaceHullmod {

    public static final float CAPACITY_PER = 0.04f;
    public static final float DISSIPATION_PER = 0.04f;
    public static final float INCREASED_DMOD_PROB = 9f;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return getDefaultIntegrationCost(member, 60000f, 200000f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant() == null) return;
        int sCount = stats.getVariant().getSMods().size();
        int dCount = DModManager.getNumDMods(stats.getVariant());
        stats.getFluxCapacity().modifyPercent(id, 100f * CAPACITY_PER * dCount);
        stats.getFluxDissipation().modifyPercent(id, 100f * DISSIPATION_PER * sCount);
        stats.getDynamic().getMod(Stats.DMOD_ACQUIRE_PROB_MOD).modifyPercent(id, 100f * INCREASED_DMOD_PROB);
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.betaCoreIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(CAPACITY_PER),
                Utils.asPercent(DISSIPATION_PER),
                Utils.asPercent(INCREASED_DMOD_PROB));
    }
}
