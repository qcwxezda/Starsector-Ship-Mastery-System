package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import shipmastery.campaign.skills.HiddenEffectScript;

public class CrystallinePseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {

    public static final int LEVEL = 6;
    public static final float BASE_DP_MULT = 3.5f;

    @Override
    public String getCommodityId() {
        return "sms_crystalline_pseudocore";
    }

    public int getBaseLevel() {
        return LEVEL;
    }

    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2f);
        stats.setSkillLevel(CRYSTALLINE_KNOWLEDGE_ID, 2f);
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_crystalline_pseudocore.png";
    }

    @Override
    public float getDurationSeconds(ShipAPI ship) {
        return 4f;
    }

    @Override
    public float getCooldownSeconds(ShipAPI ship) {
        return 36f;
    }
}
