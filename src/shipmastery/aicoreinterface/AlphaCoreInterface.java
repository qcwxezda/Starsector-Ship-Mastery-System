package shipmastery.aicoreinterface;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class AlphaCoreInterface implements AICoreInterfacePlugin {

    public static final float STAT_BOOST = 0.12f;
    public static final int S_REDUCTION = 1;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return AICoreInterfacePlugin.getDefaultIntegrationCost(member, 100000f, 200000f);
    }

    @Override
    public String getCannotIntegrateReason(FleetMemberAPI member) {
        if (Misc.getCurrSpecialMods(member.getVariant()) > 0)  {
            return Strings.Items.alphaCoreIntegrationCannotAdd;
        }
        return null;
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.alphaCoreIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Utils.asPercent(STAT_BOOST),
                Utils.asInt(S_REDUCTION));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHullBonus().modifyMult(id, 1f + STAT_BOOST);
        stats.getArmorBonus().modifyMult(id, 1f + STAT_BOOST);
        stats.getFluxDissipation().modifyMult(id, 1f + STAT_BOOST);
        stats.getFluxCapacity().modifyMult(id, 1f + STAT_BOOST);
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, -1f);
    }
}
