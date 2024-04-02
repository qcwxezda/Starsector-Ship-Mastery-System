package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.ShipMastery;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

/** Note: will just give a flat 10% OP bonus for characters that aren't the player, if for some reason
 *  an NPC manages to have this skill. */
public class CyberneticAugmentation {

    public static final String MASTERED_COUNT_KEY = "$sms_MasteredCountKey";
    public static final float NPC_OP_BONUS = 0.1f;
    public static final float BASE_BONUS = 0.05f;
    public static final float BONUS_PER_MASTERED_CLUSTER = 0.01f;
    public static final int MASTERIES_PER_CLUSTER = 3;
    public static final float MAX_BONUS = 0.15f;

    public static class Level0 implements DescriptionSkillEffect {
        public String getString() {
            int base = Global.getSettings().getInt("officerMaxEliteSkills");
            Integer count = (Integer) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get(MASTERED_COUNT_KEY);
            if (count == null) count = 0;
            return String.format(Strings.Misc.cyberneticAugmentationDesc2, base) + "\n" + String.format(Strings.Misc.cyberneticAugmentationDesc3, count);
        }
        public Color[] getHighlightColors() {
            Color h = Misc.getHighlightColor();
            return new Color[] {h, h};
        }
        public String[] getHighlights() {
            int base = Global.getSettings().getInt("officerMaxEliteSkills");
            Integer count = (Integer) Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().get(MASTERED_COUNT_KEY);
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
            info.addPara(Strings.Misc.scopePrefix, opad + 5f, Misc.getGrayColor(), c, Strings.Misc.cyberneticAugmentationScope);
            info.addSpacer(opad);
            PersonAPI player = Global.getSector().getPlayerPerson();
            Integer count = (Integer) player.getMemoryWithoutUpdate().get(MASTERED_COUNT_KEY);
            if (count == null) count = 0;
            int clusters = count / MASTERIES_PER_CLUSTER;
            float bonus = Math.min(MAX_BONUS, BASE_BONUS + clusters * BONUS_PER_MASTERED_CLUSTER);
            info.addPara(Strings.Misc.cyberneticAugmentationDesc, 0f, hc, hc,
                         Utils.asPercent(bonus),
                         Utils.asPercent(BASE_BONUS),
                         Utils.asPercent(BONUS_PER_MASTERED_CLUSTER),
                         Utils.asInt(MASTERIES_PER_CLUSTER),
                         Utils.asPercent(MAX_BONUS));
        }

        @Override
        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            if (!stats.isPlayerStats()) {
                stats.getShipOrdnancePointBonus().modifyPercent(id, 100f * NPC_OP_BONUS);
                return;
            }

            PersonAPI player = Global.getSector().getPlayerPerson();
            Integer masteredCount = (Integer) player.getMemoryWithoutUpdate().get(MASTERED_COUNT_KEY);
            if (masteredCount == null || masteredCount <= 0) return;

            int clusters = masteredCount / MASTERIES_PER_CLUSTER;
            float bonus = Math.min(MAX_BONUS, BASE_BONUS + clusters * BONUS_PER_MASTERED_CLUSTER);
            stats.getShipOrdnancePointBonus().modifyPercent(id, 100f * bonus);
        }

        @Override
        public void unapply(MutableCharacterStatsAPI stats, String id) {
            stats.getShipOrdnancePointBonus().unmodify(id);
        }
    }

    public static void refreshPlayerMasteredCount() {
        int count = 0;
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            ShipHullSpecAPI restoredSpec = Utils.getRestoredHullSpec(spec);
            if (spec != restoredSpec) continue;

            if (ShipMastery.getPlayerMasteryLevel(spec) >= ShipMastery.getMaxMasteryLevel(spec)) {
                count++;
            }
        }
        Global.getSector().getPlayerPerson().getMemoryWithoutUpdate().set(MASTERED_COUNT_KEY, count);
    }
}
