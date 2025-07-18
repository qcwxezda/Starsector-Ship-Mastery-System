package shipmastery.campaign.items;

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
}
