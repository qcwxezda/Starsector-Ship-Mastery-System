package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class AmorphousCorePlugin extends BaseKCorePlugin {
    public static final int LEVEL = 9;
    public static final float BASE_DP_MULT = 7.5f;
    public static final float MIN_DP_MULT = 4.5f;
    public static final float DP_MULT_PER_ENHANCE = 0.3f;
    public static final String DIMENSIONAL_TETHER_ID = "sms_dimensional_tether";

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2f);
        stats.setSkillLevel(Skills.COMBAT_ENDURANCE, 2f);
        stats.setSkillLevel(Skills.DAMAGE_CONTROL, 2f);
        stats.setSkillLevel(SHARED_KNOWLEDGE_ID, 2f);
        stats.setSkillLevel(DIMENSIONAL_TETHER_ID, 2f);
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Strings.Items.kCorePersonalityText,
                Misc.getHighlightColor(),
                spec.getName());
        tooltip.addPara(Strings.Items.amorphousCorePersonalityText,
                5f,
                Misc.getTextColor(),
                Misc.getHighlightColor(),
                Utils.asFloatOneDecimal(DP_MULT_PER_ENHANCE) + "x");
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
        return "sms_amorphous_core";
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_amorphous_core.png";
    }
}
