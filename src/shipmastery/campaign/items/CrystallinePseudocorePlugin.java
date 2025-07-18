package shipmastery.campaign.items;

import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.campaign.skills.HiddenEffectScript;

import java.util.List;

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

    @Override
    public List<String> getPrioritySkills() {
        var ids = super.getPrioritySkills();
        ids.remove(SHARED_KNOWLEDGE_ID);
        ids.add(0, CRYSTALLINE_KNOWLEDGE_ID);
        return ids;
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
