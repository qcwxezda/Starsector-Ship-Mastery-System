package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import shipmastery.campaign.skills.SharedKnowledge;

public class BetaPseudocorePlugin extends BasePseudocorePlugin implements SharedKnowledge.HiddenAICoreEffect {

    public static final int LEVEL = 5;
    public static final float BASE_DP_MULT = 3f;

    @Override
    public String getCommodityId() {
        return "sms_beta_pseudocore";
    }

    public int getBaseLevel() {
        return LEVEL;
    }

    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(SHARED_KNOWLEDGE_ID, 2f);
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_beta_pseudocore.png";
    }

    @Override
    public float getCooldownSeconds(ShipAPI ship) {
        return 45f;
    }
}
