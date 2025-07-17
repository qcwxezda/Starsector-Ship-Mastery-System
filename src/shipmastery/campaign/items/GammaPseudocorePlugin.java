package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import shipmastery.campaign.skills.HiddenEffectScript;

public class GammaPseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {
    public static final int LEVEL = 3;
    public static final float DP_MULT = 2f;

    @Override
    public float getBaseAIPointsMult() {
        return DP_MULT;
    }

    @Override
    public int getBaseLevel() {
        return LEVEL;
    }

    @Override
    public String getCommodityId() {
        return "sms_gamma_pseudocore";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_gamma_pseudocore.png";
    }

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(LEVEL);
        stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(SHARED_KNOWLEDGE_ID, 2f);
    }
}
