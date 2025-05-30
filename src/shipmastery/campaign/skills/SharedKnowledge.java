package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.util.CampaignUtils;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

// NPCs don't have enhances, so treat every NPC fleet as having 0 enhances.
public class SharedKnowledge {
    public static final float BASE_DAMAGE_BONUS = 0.005f;
    public static final float DAMAGE_BONUS_PER_LEVEL = 0.005f;
    public static final float MAX_DAMAGE_BONUS = 0.05f;
    public static final float DP_REDUCTION_PER_ENHANCE = 0.015f;
    // In practice cap is 0.15f since 10 enhances is max, don't need additional cap
    public static final float MAX_DP_REDUCTION_ENHANCE = 1f;
    public static final float BASE_DP_REDUCTION_AI_COMMANDER = 0.15f;
    public static final float BASE_DP_REDUCTION_HUMAN_COMMANDER = 0.05f;

    public static int getMasteryLevel(PersonAPI fleetCommander, MutableShipStatsAPI stats) {
        if (fleetCommander == null || fleetCommander.isDefault()) return 0;
        if (fleetCommander.isPlayer()) {
            return ShipMastery.getPlayerMasteryLevel(stats.getVariant().getHullSpec());
        } else {
            var masteries = FleetHandler.getCachedNPCMasteries(fleetCommander, stats.getVariant().getHullSpec());
            if (masteries == null || masteries.isEmpty()) return 0;
            return masteries.lastKey();
        }
    }

    public static int getEnhanceCount(MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || stats.getVariant().getHullSpec() == null) return 0;
        var commander = CampaignUtils.getFleetCommanderForStats(stats);
        if (commander == null || !commander.isPlayer()) {
            return 0;
        }
        return MasteryUtils.getEnhanceCount(stats.getVariant().getHullSpec());
    }

    public static boolean hasEliteSharedKnowledge(PersonAPI person) {
        if (person == null || person.getStats() == null) return false;
        return person.getStats().getSkillLevel("sms_shared_knowledge") >= 2f;
    }

    public static class Standard extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            PersonAPI commander = CampaignUtils.getFleetCommanderForStats(stats);
            int masteryLevel = getMasteryLevel(commander, stats);
            float bonus = Math.min(MAX_DAMAGE_BONUS, BASE_DAMAGE_BONUS + masteryLevel*DAMAGE_BONUS_PER_LEVEL);
            PersonAPI captain = CampaignUtils.getCaptain(stats);
            if (captain != null && captain.isAICore()) {
                bonus *= 2f;
            }
            if (bonus <= 0f) return;
            stats.getHullDamageTakenMult().modifyMult(id, 1f-bonus);
            stats.getArmorDamageTakenMult().modifyMult(id, 1f-bonus);
            stats.getShieldDamageTakenMult().modifyMult(id, 1f-bonus);
            stats.getBallisticWeaponDamageMult().modifyPercent(id, 100f*bonus);
            stats.getEnergyWeaponDamageMult().modifyPercent(id, 100f*bonus);
            stats.getMissileWeaponDamageMult().modifyPercent(id, 100f*bonus);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getShieldDamageTakenMult().unmodify(id);
            stats.getBallisticWeaponDamageMult().unmodify(id);
            stats.getEnergyWeaponDamageMult().unmodify(id);
            stats.getMissileWeaponDamageMult().unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            // Needed because codex doesn't like \n character
            info.addPara(Strings.Skills.sharedKnowledgeStandardEffect, 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                    Utils.asPercent(BASE_DAMAGE_BONUS),
                    Utils.asPercent(DAMAGE_BONUS_PER_LEVEL),
                    Utils.asPercent(MAX_DAMAGE_BONUS),
                    Utils.asPercent(-BASE_DAMAGE_BONUS),
                    Utils.asPercent(DAMAGE_BONUS_PER_LEVEL),
                    Utils.asPercent(-MAX_DAMAGE_BONUS));
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            PersonAPI commander = CampaignUtils.getFleetCommanderForStats(stats);
            float dpReduction = 0f;
            boolean hasEliteSharedKnowledge = hasEliteSharedKnowledge(commander);
            boolean isAICore = commander != null && commander.isAICore();

            if (isAICore && hasEliteSharedKnowledge) {
                dpReduction = BASE_DP_REDUCTION_AI_COMMANDER;
            } else if (hasEliteSharedKnowledge) {
                dpReduction = BASE_DP_REDUCTION_HUMAN_COMMANDER;
            }

            int enhanceCount = getEnhanceCount(stats);
            dpReduction = Math.max(dpReduction, Math.min(DP_REDUCTION_PER_ENHANCE*enhanceCount, MAX_DP_REDUCTION_ENHANCE));

            if (dpReduction <= 0f) return;
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 1f-dpReduction);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            // Need custom description due to elite effect taking multiple lines
            info.addPara(Strings.Skills.sharedKnowledgeEliteEffect, 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                    Utils.asPercent(BASE_DP_REDUCTION_AI_COMMANDER),
                    Utils.asPercent(BASE_DP_REDUCTION_HUMAN_COMMANDER),
                    Utils.asPercent(DP_REDUCTION_PER_ENHANCE));
        }
    }
}
