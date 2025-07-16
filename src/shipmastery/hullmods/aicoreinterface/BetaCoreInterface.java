package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class BetaCoreInterface extends AICoreInterfaceHullmod {

    public static final float[] CAPACITY_PER = new float[] {200f, 400f, 600f, 800f};
    public static final float[] DISSIPATION_PER = new float[] {10f, 20f, 30f, 40f};
    public static final float INCREASED_DMOD_PROB = 9f;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return getDefaultIntegrationCost(member, 60000f, 200000f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getVariant() == null) return;
        int sCount = Misc.getCurrSpecialMods(stats.getVariant());
        int dCount = DModManager.getNumDMods(stats.getVariant());
        int size = Utils.hullSizeToInt(hullSize);
        stats.getFluxCapacity().modifyFlat(id, CAPACITY_PER[size] * dCount);
        stats.getFluxDissipation().modifyFlat(id, DISSIPATION_PER[size] * sCount);
        stats.getDynamic().getMod(Stats.DMOD_ACQUIRE_PROB_MOD).modifyPercent(id, 100f * INCREASED_DMOD_PROB);
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.betaCoreIntegrationEffect,
                0f,
                new Color[] {
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.POSITIVE_HIGHLIGHT_COLOR,
                        Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asInt(CAPACITY_PER[0]),
                Utils.asInt(CAPACITY_PER[1]),
                Utils.asInt(CAPACITY_PER[2]),
                Utils.asInt(CAPACITY_PER[3]),
                Utils.asInt(DISSIPATION_PER[0]),
                Utils.asInt(DISSIPATION_PER[1]),
                Utils.asInt(DISSIPATION_PER[2]),
                Utils.asInt(DISSIPATION_PER[3]),
                Utils.asPercent(INCREASED_DMOD_PROB));
    }
}
