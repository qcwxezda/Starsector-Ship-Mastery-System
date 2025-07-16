package shipmastery.hullmods.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.campaign.skills.SharedKnowledge;
import shipmastery.config.Settings;
import shipmastery.util.CampaignUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class AlphaPseudocoreInterface extends AICoreInterfaceHullmod {

    public static final float FIRE_RATE_INCREASE = SharedKnowledge.Elite.MAX_FIRE_RATE;
    public static final float CR_REDUCTION = 0.1f;

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(Strings.Items.alphaPseudocoreIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Global.getSettings().getSkillSpec("sms_shared_knowledge").getName(),
                Utils.asPercent(FIRE_RATE_INCREASE),
                Utils.asPercent(CR_REDUCTION));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(SharedKnowledge.Elite.FIRE_RATE_STACKS_MOD).modifyMult(id, 9999f);
        var captain = CampaignUtils.getCaptain(stats);
        boolean hasEliteSharedKnowledge = captain != null && captain.getStats() != null && captain.getStats().getSkillLevel("sms_shared_knowledge") >= 2f;
        if (!hasEliteSharedKnowledge) {
            SharedKnowledge.Elite.applyEffectsToStats(stats, FIRE_RATE_INCREASE, id);
            stats.getMaxCombatReadiness().modifyFlat(id, -CR_REDUCTION, Strings.Items.integratedDesc);
        }
    }

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return getDefaultIntegrationCost(member, 150000f, 500000f);
    }
}
