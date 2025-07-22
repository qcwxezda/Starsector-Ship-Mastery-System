package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.MasteryEffect;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

@Deprecated
public class BestOfTheBest {

    public static class Level0 implements DescriptionSkillEffect {
        public String getString() {
            return Strings.Skills.bestOfTheBestDesc3;
        }
        public Color[] getHighlightColors() {
            return null;
        }
        public String[] getHighlights() {
            return null;
        }
        public Color getTextColor() {
            return null;
        }
    }

    public static final float MASTERY_BONUS = 0.25f;
    public static class Level1 implements CharacterStatsSkillEffect {
        @Override
        public String getEffectDescription(float level) {
            return String.format(Strings.Skills.bestOfTheBestDesc, Utils.asPercent(MASTERY_BONUS));
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.ALL_SHIPS;
        }

        @Override
        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            stats.getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD).modifyPercent(id, 100f * MASTERY_BONUS);
        }

        @Override
        public void unapply(MutableCharacterStatsAPI stats, String id) {
            stats.getDynamic().getMod(MasteryEffect.GLOBAL_MASTERY_STRENGTH_MOD).unmodify(id);
        }
    }

    // from WolfpackTactics
    public static boolean isCapitalAndOfficer(MutableShipStatsAPI stats) {
        if (stats.getEntity() instanceof ShipAPI ship) {
            if (!ship.isCapital()) return false;
            return !ship.getCaptain().isDefault();
        } else {
            FleetMemberAPI member = stats.getFleetMember();
            if (member == null) return false;
            if (!member.isCapital()) return false;
            return !member.getCaptain().isDefault();
        }
    }


    public static final float CR_BONUS = 0.15f, STATS_BONUS = 0.1f;
    public static class Level3 extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            if (isCapitalAndOfficer(stats)) {
                stats.getMaxCombatReadiness().modifyFlat(id, CR_BONUS, Strings.Skills.bestOfTheBestCRDesc);
                stats.getHullBonus().modifyMult(id, 1f + STATS_BONUS);
                stats.getFluxCapacity().modifyMult(id, 1f + STATS_BONUS);
            }
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getMaxCombatReadiness().unmodify(id);
            stats.getHullBonus().unmodify(id);
            stats.getFluxCapacity().unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info,
                                            float width) {
            init(stats, skill);
            float opad = 10f;
			Color c = Misc.getBasePlayerColor();
            info.addPara(Strings.Misc.scopePrefix, opad + 5f, Misc.getGrayColor(), c, Strings.Skills.bestOfTheBestScope);
			info.addSpacer(opad);
            info.addPara(Strings.Skills.bestOfTheBestDesc2, 0f, hc, hc, Utils.asPercent(CR_BONUS), Utils.asPercent(STATS_BONUS), Utils.asPercent(STATS_BONUS));
        }

        @Override
        public String getEffectDescription(float level) {
            return null;
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.ALL_COMBAT_SHIPS;
        }
    }
}
