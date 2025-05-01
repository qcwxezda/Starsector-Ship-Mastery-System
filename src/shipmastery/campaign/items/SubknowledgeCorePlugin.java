package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

public class SubknowledgeCorePlugin extends KnowledgeCorePlugin {

    public static final float DP_MULT = 2.5f;
    public static final int MAX_LEVEL = 4;

    @Override
    public float getAIPointsMult() {
        return DP_MULT;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_subknowledge_core.png";
    }

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(MAX_LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(AmorphousCorePlugin.UNIQUE_SKILL_ID, 2f);
    }
}
