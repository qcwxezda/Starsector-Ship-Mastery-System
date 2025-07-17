package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.campaign.skills.HiddenEffectScript;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class AmorphousPseudocorePlugin extends BasePseudocorePlugin implements HiddenEffectScript.Provider {
    public static final int LEVEL = 9;
    public static final float BASE_DP_MULT = 5f;
    public static final float MIN_DP_MULT = 1f;
    public static final float DP_MULT_PER_MP_GROUP = 0.01f;
    public static final float MP_PER_GROUP = 25f;
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
        stats.setSkillLevel(AMORPHOUS_KNOWLEDGE_ID, 2f);
        stats.setSkillLevel(DIMENSIONAL_TETHER_ID, 2f);
    }

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
    public float getCooldownSeconds(ShipAPI ship) {
        return 24f;
    }
}
