package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.DescriptionSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.achievements.MasteredMany;
import shipmastery.config.Settings;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

/** Note: will just give a flat 5% OP bonus for characters that aren't the player, if for some reason
 *  an NPC manages to have this skill. */
@Deprecated
public class CyberneticAugmentation {

    public static final float NPC_OP_BONUS = 0.05f;
    public static final int MASTERIES_PER_CLUSTER = 5;

    public static class Level0 implements DescriptionSkillEffect {
        public String getString() {
            int base = Global.getSettings().getInt("officerMaxEliteSkills");
            Integer count = (Integer) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get(MasteredMany.MASTERED_COUNT_KEY);
            if (count == null) count = 0;
            String masterDesc = count == 1 ? Strings.Skills.cyberneticAugmentationDesc3Singular : Strings.Skills.cyberneticAugmentationDesc3;
            return String.format(Strings.Skills.cyberneticAugmentationDesc2, base) + "\n" + String.format(masterDesc, count);
        }
        public Color[] getHighlightColors() {
            Color h = Misc.getHighlightColor();
            return new Color[] {h, h};
        }
        public String[] getHighlights() {
            int base = Global.getSettings().getInt("officerMaxEliteSkills");
            Integer count = (Integer) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get(MasteredMany.MASTERED_COUNT_KEY);
            if (count == null) count = 0;
            return new String[] {"" + base, "" + count};
        }
        public Color getTextColor() {
            return null;
        }
    }

    public static class Level3 extends BaseSkillEffectDescription implements CharacterStatsSkillEffect {

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info,
                                            float width) {
            init(stats, skill);
            float opad = 10f;
            Color c = Misc.getBasePlayerColor();
            info.addPara(Strings.Misc.scopePrefix, opad + 5f, Misc.getGrayColor(), c, Strings.Skills.cyberneticAugmentationScope);
            info.addSpacer(opad);
            PersonAPI player = Global.getSector().getPlayerPerson();
            Integer count = (Integer) player.getMemoryWithoutUpdate().get(MasteredMany.MASTERED_COUNT_KEY);
            if (count == null) count = 0;
            int clusters = count / MASTERIES_PER_CLUSTER;
            float bonus = Math.min(Settings.CYBER_AUG_MAX_BONUS, Settings.CYBER_AUG_BASE_BONUS + clusters * Settings.CYBER_AUG_BONUS_PER_GROUP);
            info.addPara(Strings.Skills.cyberneticAugmentationDesc, 0f, hc, hc,
                         Utils.asPercent(bonus),
                         Utils.asPercent(Settings.CYBER_AUG_BASE_BONUS),
                         Utils.asPercent(Settings.CYBER_AUG_BONUS_PER_GROUP),
                         Utils.asInt(MASTERIES_PER_CLUSTER),
                         Utils.asPercent(Settings.CYBER_AUG_MAX_BONUS));
        }

        @Override
        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            if (!stats.isPlayerStats()) {
                stats.getShipOrdnancePointBonus().modifyPercent(id, 100f * NPC_OP_BONUS);
                return;
            }

            PersonAPI player = Global.getSector().getPlayerPerson();
            Integer masteredCount = (Integer) player.getMemoryWithoutUpdate().get(MasteredMany.MASTERED_COUNT_KEY);
            if (masteredCount == null || masteredCount < 0) return;

            int clusters = masteredCount / MASTERIES_PER_CLUSTER;
            float bonus = Math.min(Settings.CYBER_AUG_MAX_BONUS, Settings.CYBER_AUG_BASE_BONUS + clusters * Settings.CYBER_AUG_BONUS_PER_GROUP);
            stats.getShipOrdnancePointBonus().modifyPercent(id, 100f * bonus);
        }

        @Override
        public void unapply(MutableCharacterStatsAPI stats, String id) {
            stats.getShipOrdnancePointBonus().unmodify(id);
        }
    }

}
