package shipmastery.campaign.items;

import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.campaign.skills.HiddenEffectScript;

public class AlphaPseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {

    public static final int LEVEL = 7;
    public static final float BASE_DP_MULT = 4f;

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
        return "sms_alpha_pseudocore";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_alpha_pseudocore.png";
    }

    @Override
    public float getCooldownSeconds(ShipAPI ship) {
        return 30f;
    }
}
