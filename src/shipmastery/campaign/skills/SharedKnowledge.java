package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.ShipMastery;
import shipmastery.campaign.FleetHandler;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.VariantLookup;

// NPCs don't have enhances, so treat every NPC fleet as having 5 enhances.
public class SharedKnowledge {
    public static final int NPC_ENHANCE_COUNT = 5;
    public static final float DAMAGE_BONUS_PER_LEVEL = 0.01f;
    public static final float MAX_DAMAGE_BONUS = 0.1f;
    public static final float TIMEFLOW_BONUS_PER_ENHANCE = 0.01f;
    public static final float MAX_TIMEFLOW_BONUS = 0.1f;

    private static PersonAPI getCommander(MutableShipStatsAPI stats) {
        if (stats.getVariant() == null) return null;
        var info = VariantLookup.getVariantInfo(stats.getVariant());
        return info.commander;
    }

    public static int getMasteryLevel(MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || stats.getVariant().getHullSpec() == null) return 0;
        var commander = getCommander(stats);
        if (commander == null || commander.isDefault()) return 0;
        if (commander.isPlayer()) {
            return ShipMastery.getPlayerMasteryLevel(stats.getVariant().getHullSpec());
        } else {
            var masteries = FleetHandler.getCachedNPCMasteries(commander, stats.getVariant().getHullSpec());
            if (masteries == null || masteries.isEmpty()) return 0;
            return masteries.lastKey();
        }
    }

    public static int getEnhanceCount(MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || stats.getVariant().getHullSpec() == null) return 0;
        var commander = getCommander(stats);
        if (commander == null || !commander.isPlayer()) {
            return NPC_ENHANCE_COUNT;
        }
        return MasteryUtils.getEnhanceCount(stats.getVariant().getHullSpec());
    }

    public static class Standard implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            int masteryLevel = getMasteryLevel(stats);
            float bonus = Math.min(MAX_DAMAGE_BONUS, masteryLevel*DAMAGE_BONUS_PER_LEVEL);
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
        public String getEffectDescription(float level) {
            return Strings.Skills.sharedKnowledgeStandardEffect;
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static class Elite implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            int enhanceCount = getEnhanceCount(stats);
            float bonus = Math.min(MAX_TIMEFLOW_BONUS, enhanceCount*TIMEFLOW_BONUS_PER_ENHANCE);
            if (bonus <= 0f) return;
            stats.getTimeMult().modifyPercent(id, 100f*bonus);
            stats.getEnergyWeaponRangeBonus().modifyMult(id, 1f-bonus);
            stats.getBallisticWeaponRangeBonus().modifyMult(id, 1f-bonus);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getTimeMult().unmodify(id);
            stats.getEnergyWeaponRangeBonus().unmodify(id);
            stats.getBallisticWeaponRangeBonus().unmodify(id);
        }

        @Override
        public String getEffectDescription(float level) {
            return Strings.Skills.sharedKnowledgeEliteEffect;
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

}
