package shipmastery.campaign.items;

import shipmastery.campaign.skills.HiddenEffectScript;

import java.util.List;

public class WarpedPseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {

    public static final int LEVEL = 6;
    public static final float BASE_DP_MULT = 3.5f;

    @Override
    public String getCommodityId() {
        return "sms_warped_pseudocore";
    }

    public int getBaseLevel() {
        return LEVEL;
    }

    @Override
    public List<String> getPrioritySkills() {
        var ids = super.getPrioritySkills();
        ids.remove(SHARED_KNOWLEDGE_ID);
        ids.add(0, WARPED_KNOWLEDGE_ID);
        return ids;
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_warped_pseudocore.png";
    }

    @Override
    public float getBaseDurationSeconds() {
        return 4f;
    }

    @Override
    public float getBaseCooldownSeconds() {
        return 30f;
    }
}
