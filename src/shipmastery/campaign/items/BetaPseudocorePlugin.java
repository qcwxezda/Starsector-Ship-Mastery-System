package shipmastery.campaign.items;

import shipmastery.campaign.skills.HiddenEffectScript;

public class BetaPseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {

    public static final int LEVEL = 5;
    public static final float BASE_DP_MULT = 3f;

    @Override
    public String getCommodityId() {
        return "sms_beta_pseudocore";
    }

    public int getBaseLevel() {
        return LEVEL;
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_beta_pseudocore.png";
    }

    @Override
    public float getBaseCooldownSeconds() {
        return 45f;
    }
}
