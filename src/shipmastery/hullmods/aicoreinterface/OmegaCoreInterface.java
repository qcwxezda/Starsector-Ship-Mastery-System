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

public class OmegaCoreInterface extends AICoreInterfaceHullmod {

    public static final float CR_INCREASE = 1f;
    public static final int S_CAPACITY = 1;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return 5000000f;
    }

    @Override
    public String getCannotRemoveReason(FleetMemberAPI member) {
        if (Misc.getCurrSpecialMods(member.getVariant()) > 0) {
            return Strings.Items.betaPseudocoreIntegrationCannotRemove;
        }
        return super.getCannotRemoveReason(member);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxCombatReadiness().modifyFlat(id, CR_INCREASE, Strings.Items.integratedDesc);
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, S_CAPACITY);
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.omegaCoreIntegrationEffect, 0f, Settings.POSITIVE_HIGHLIGHT_COLOR, Utils.asPercent(CR_INCREASE), Utils.asInt(S_CAPACITY));
    }
}
