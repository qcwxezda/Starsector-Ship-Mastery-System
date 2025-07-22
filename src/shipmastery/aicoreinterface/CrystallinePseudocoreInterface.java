package shipmastery.aicoreinterface;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.campaign.skills.CrystallineKnowledge;
import shipmastery.config.Settings;
import shipmastery.util.CampaignUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class CrystallinePseudocoreInterface implements AICoreInterfacePlugin {

    public static final float DURATION_INCREASE = 1f;
    public static final float PER_STACK_INCREASE = 0.5f;
    public static final float DEBUFF_CAP_INCREASE = 0.25f;
    public static final float DAMAGE_TAKEN_REDUCTION = 0.12f;
    public static final float CR_REDUCTION = 0.08f;

    @Override
    public float getIntegrationCost(FleetMemberAPI member) {
        return AICoreInterfacePlugin.getDefaultIntegrationCost(member, 100000f, 300000f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(CrystallineKnowledge.Elite.STACK_STRENGTH_MOD).modifyPercent(id, 100f * PER_STACK_INCREASE);
        stats.getDynamic().getMod(CrystallineKnowledge.Elite.DURATION_MOD).modifyPercent(id, 100f * DURATION_INCREASE);
        stats.getDynamic().getMod(CrystallineKnowledge.Elite.DEBUFF_CAP_MOD).modifyPercent(id, 100f * DEBUFF_CAP_INCREASE);

        var captain = CampaignUtils.getCaptain(stats);
        boolean hasEliteCrystallineKnowledge = CrystallineKnowledge.hasEliteCrystallineKnowledge(captain);
        if (!hasEliteCrystallineKnowledge) {
            stats.getKineticShieldDamageTakenMult().modifyMult(id, 1f-DAMAGE_TAKEN_REDUCTION);
            stats.getHighExplosiveDamageTakenMult().modifyMult(id, 1f-DAMAGE_TAKEN_REDUCTION);
            stats.getFragmentationDamageTakenMult().modifyMult(id, 1f-DAMAGE_TAKEN_REDUCTION);
            stats.getFragmentationShieldDamageTakenMult().modifyMult(id, 1f-DAMAGE_TAKEN_REDUCTION);
            stats.getMaxCombatReadiness().modifyFlat(id, -CR_REDUCTION, Strings.Items.integratedDesc);
        }
    }

    @Override
    public void addIntegrationDescriptionToTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(
                Strings.Items.crystallinePseudocoreIntegrationEffect,
                0f,
                new Color[] {Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.POSITIVE_HIGHLIGHT_COLOR, Settings.NEGATIVE_HIGHLIGHT_COLOR},
                Global.getSettings().getSkillSpec("sms_crystalline_knowledge").getName(),
                Utils.asPercent(PER_STACK_INCREASE),
                Utils.asPercent(DEBUFF_CAP_INCREASE),
                Utils.asPercent(DURATION_INCREASE),
                Utils.asPercent(DAMAGE_TAKEN_REDUCTION),
                Utils.asPercent(CR_REDUCTION));
    }
}
