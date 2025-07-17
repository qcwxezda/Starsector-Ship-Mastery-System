package shipmastery.hullmods.aicoreinterface;

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

public class AlphaCoreInterface extends AICoreInterfaceHullmod {

    public static final float STAT_BOOST = 0.1f;
    public static final int S_REDUCTION = 1;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return getDefaultIntegrationCost(member, 125000f, 400000f);
    }

    @Override
    public String getCannotIntegrateReason(FleetMemberAPI member) {
        if (Misc.getCurrSpecialMods(member.getVariant()) > 0)  {
            return Strings.Items.alphaCoreIntegrationCannotAdd;
        }
        return super.getCannotRemoveReason(member);
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
        stats.getHullBonus().modifyPercent(id, 100f * STAT_BOOST);
        stats.getArmorBonus().modifyPercent(id, 100f * STAT_BOOST);
        stats.getFluxDissipation().modifyPercent(id, 100f * STAT_BOOST);
        stats.getFluxCapacity().modifyPercent(id, 100f * STAT_BOOST);
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, -1f);
    }
}
