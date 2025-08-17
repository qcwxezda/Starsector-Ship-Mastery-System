package shipmastery.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.campaign.skills.WarpedKnowledge;
import shipmastery.config.Settings;
import shipmastery.util.CampaignUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class WarpedPseudocoreInterface implements AICoreInterfacePlugin {

    public static final float DURATION_INCREASE = 1f;
    public static final float PER_STACK_INCREASE = 0.5f;
    public static final float DEBUFF_CAP_INCREASE = 0.25f;
    public static final float DAMAGE_DEALT_INCREASE = 0.1f;
    public static final float CR_REDUCTION = 0.05f;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return AICoreInterfacePlugin.getDefaultIntegrationCost(member, 100000f, 200000f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(WarpedKnowledge.Elite.STACK_STRENGTH_MOD).modifyPercent(id, 100f * PER_STACK_INCREASE);
        stats.getDynamic().getMod(WarpedKnowledge.Elite.DURATION_MOD).modifyPercent(id, 100f * DURATION_INCREASE);
        stats.getDynamic().getMod(WarpedKnowledge.Elite.DEBUFF_CAP_MOD).modifyPercent(id, 100f * DEBUFF_CAP_INCREASE);
        var captain = CampaignUtils.getCaptain(stats);
        boolean hasEliteWarpedKnowledge = WarpedKnowledge.hasEliteWarpedKnowledge(captain);
        if (!hasEliteWarpedKnowledge) {
            stats.getMaxCombatReadiness().modifyFlat(id, -CR_REDUCTION, Strings.Items.integratedDesc);
        }
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        boolean hasEliteWarpedKnowledge = WarpedKnowledge.hasEliteWarpedKnowledge(ship.getCaptain());
        if (!hasEliteWarpedKnowledge) {
            ship.addListener((DamageDealtModifier) (param, target, damage, point, shieldHit) -> {
                boolean affected = switch (damage.getType()) {
                    case KINETIC -> shieldHit;
                    case HIGH_EXPLOSIVE -> !shieldHit;
                    case ENERGY -> true;
                    default -> false;
                };
                if (affected) {
                    damage.getModifier().modifyPercent(id, 100f * DAMAGE_DEALT_INCREASE);
                    return id;
                }
                return null;
            });
        }
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(
                Strings.Items.warpedPseudocoreIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Global.getSettings().getSkillSpec("sms_warped_knowledge").getName(),
                Utils.asPercent(PER_STACK_INCREASE),
                Utils.asPercent(DEBUFF_CAP_INCREASE),
                Utils.asPercent(DURATION_INCREASE),
                Utils.asPercent(DAMAGE_DEALT_INCREASE),
                Utils.asPercent(CR_REDUCTION));
    }
}
