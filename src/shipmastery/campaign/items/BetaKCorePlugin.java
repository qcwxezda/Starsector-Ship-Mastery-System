package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

public class BetaKCorePlugin extends BaseKCorePlugin {

    public static final int LEVEL = 5;
    public static final float BASE_DP_MULT = 3.5f;

    @Override
    public String getCommodityId() {
        return "sms_beta_k_core";
    }

    public int getBaseLevel() {
        return LEVEL;
    }

    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(SHARED_KNOWLEDGE_ID, 2f);
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_beta_k_core.png";
    }
}
