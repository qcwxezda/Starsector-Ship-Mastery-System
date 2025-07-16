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

    public static final float TIME_FLOW_INCREASE = 0.12f;
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
                Utils.asPercent(TIME_FLOW_INCREASE),
                Utils.asInt(S_REDUCTION));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getTimeMult().modifyPercent(id, 100f * TIME_FLOW_INCREASE);
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, -1f);
    }
}
