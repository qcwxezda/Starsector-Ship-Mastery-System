package shipmastery.campaign.items;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;

public class FracturedGammaCorePlugin extends SubknowledgeCorePlugin {

    public static final int MAX_LEVEL = 1;
    public static final float DP_MULT = 1.5f;

    @Override
    public int getBaseLevel() {
        return MAX_LEVEL;
    }

    @Override
    public float getBaseAIPointsMult() {
        return DP_MULT;
    }

    @Override
    public String getCommodityId() {
        return "sms_fractured_gamma_core";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_fractured_gamma_core.png";
    }

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(MAX_LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
    }
}
