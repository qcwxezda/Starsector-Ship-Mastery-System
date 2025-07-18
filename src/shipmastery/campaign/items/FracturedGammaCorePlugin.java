package shipmastery.campaign.items;

import com.fs.starfarer.api.impl.campaign.ids.Skills;

import java.util.ArrayList;
import java.util.List;

public class FracturedGammaCorePlugin extends BasePseudocorePlugin {

    public static final int LEVEL = 1;
    public static final float DP_MULT = 1.33f;

    @Override
    public int getBaseLevel() {
        return LEVEL;
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
    public List<String> getPrioritySkills() {
        List<String> ids = new ArrayList<>();
        ids.add(Skills.HELMSMANSHIP);
        return ids;
    }

    @Override
    public float getEnlightenedAIMultIncrease() {
        return 0.33f;
    }
}
