package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

public class AlphaKCorePlugin extends BetaKCorePlugin {

    public static final int MIN_LEVEL = 5;
    public static final int MAX_LEVEL = 8;
    public static final float LEVELS_PER_MASTERY_LEVEL = 1f/3f;
    public static final float MIN_DP_MULT = 3.5f;
    public static final float MAX_DP_MULT = 6f;
    public static final float DP_MULT_PER_ENHANCE = 0.25f;

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        if ("player".equals(factionId)) {
            stats.setLevel(MIN_LEVEL);
            stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
            stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
            stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
            stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        } else {
            stats.setLevel(MAX_LEVEL);
            stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
            stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
            stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
            stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
            stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2f);
            stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f);
            stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2f);
        }
        stats.setSkillLevel(UNIQUE_SKILL_ID, 2f);
    }

    @Override
    public float getBaseAIPointsMult() {
        return MAX_DP_MULT;
    }

    @Override
    public int getBaseLevel() {
        return MIN_LEVEL;
    }

    @Override
    public String getCommodityId() {
        return "sms_alpha_k_core";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_alpha_k_core.png";
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Strings.Items.amorphousCorePersonalityText,
                Misc.getHighlightColor(),
                spec.getName());
    }
}
