package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

public class AlphaKCorePlugin extends BaseKCorePlugin {

    public static final int LEVEL = 7;
    public static final float BASE_DP_MULT = 4.4f;

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2f);
        stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f);
        stats.setSkillLevel(SHARED_KNOWLEDGE_ID, 2f);
    }

    @Override
    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    @Override
    public int getBaseLevel() {
        return LEVEL;
    }

    @Override
    public String getCommodityId() {
        return "sms_alpha_k_core";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_alpha_k_core.png";
    }
}
