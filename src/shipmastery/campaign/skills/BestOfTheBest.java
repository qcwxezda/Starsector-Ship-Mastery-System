package shipmastery.campaign.skills;

import com.fs.starfarer.api.characters.*;
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

public class BestOfTheBest {

    public static class Level0 implements DescriptionSkillEffect {
        public String getString() {
            return Strings.BEST_OF_THE_BEST_DESC3;
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

    public static float MASTERY_BONUS = 0.3f;
    public static class Level1 implements CharacterStatsSkillEffect {
        @Override
        public String getEffectDescription(float level) {
            return String.format(Strings.BEST_OF_THE_BEST_DESC, Utils.asPercent(MASTERY_BONUS));
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
        if (stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            if (!ship.isCapital()) return false;
            return !ship.getCaptain().isDefault();
        } else {
            FleetMemberAPI member = stats.getFleetMember();
            if (member == null) return false;
            if (!member.isCapital()) return false;
            return !member.getCaptain().isDefault();
        }
    }


    public static float STATS_BONUS = 0.1f;
    public static class Level3 extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            if (isCapitalAndOfficer(stats)) {
                stats.getMaxCombatReadiness().modifyFlat(id, STATS_BONUS, "Best of the Best skill");
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
            info.addPara(Strings.BEST_OF_THE_BEST_SCOPE, opad + 5f, Misc.getGrayColor(), c, Strings.BEST_OF_THE_BEST_SCOPE2);
			info.addSpacer(opad);
            info.addPara(Strings.BEST_OF_THE_BEST_DESC2, 0f, hc, hc, Utils.asPercent(STATS_BONUS));
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
