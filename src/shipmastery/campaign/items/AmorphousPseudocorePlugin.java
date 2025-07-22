package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.skills.HiddenEffectScript;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.List;

public class AmorphousPseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {
    public static final int LEVEL = 9;
    public static final float BASE_DP_MULT = 5f;
    public static final float MIN_DP_MULT = 1f;
    public static final float DP_MULT_PER_MP_GROUP = 0.01f;
    public static final float MP_PER_GROUP = 25f;
    public static final String DIMENSIONAL_TETHER_ID = "sms_dimensional_tether";

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Misc.getHighlightColor(),
                spec.getName());
        tooltip.addPara(Strings.Items.amorphousCorePersonalityText,
                10f,
                Misc.getTextColor(),
                Misc.getHighlightColor(),
                "×"+Utils.asFloatTwoDecimals(DP_MULT_PER_MP_GROUP),
                Utils.asFloatOneDecimal(MP_PER_GROUP),
                "x"+Utils.asFloatOneDecimal(MIN_DP_MULT));
    }

    @Override
    public List<String> getPrioritySkills() {
        var ids = super.getPrioritySkills();
        ids.remove(SHARED_KNOWLEDGE_ID);
        ids.add(0, DIMENSIONAL_TETHER_ID);
        ids.add(0, AMORPHOUS_KNOWLEDGE_ID);
        return ids;
    }

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
        return "sms_amorphous_pseudocore";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_amorphous_pseudocore.png";
    }

    @Override
    public float getBaseEffectStrength() {
        return 0.375f;
    }

    @Override
    public float getBaseCooldownSeconds() {
        return 27f;
    }
}
