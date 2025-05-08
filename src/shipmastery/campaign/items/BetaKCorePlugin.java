package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class BetaKCorePlugin extends BaseKCorePlugin {

    public static final int MAX_LEVEL = 5;
    public static final float BASE_DP_MULT = 3.5f;
    public static final float DP_MULT_PER_MISSING_SKILL = 0.5f;
    public static final float MIN_DP_MULT = 1.5f;

    @Override
    public String getCommodityId() {
        return "sms_beta_k_core";
    }

    public int getBaseLevel() {
        return MAX_LEVEL;
    }

    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(MAX_LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(UNIQUE_SKILL_ID, 2f);
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Strings.Items.kCorePersonalityText + "\n\n" + Strings.Items.kCorePersonalityText2,
                Misc.getHighlightColor(),
                spec.getName(),
                Utils.asFloatOneDecimal(MIN_DP_MULT) + "x"
        );
    }

    protected final void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip, String body, Color highlightColor, String... params) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        if (spec == null) return;

        int level;
        float autoMult;
        if (person == null) {
            String defaultPersonality = getPlayerPersonalityId();
            person = Global.getFactory().createPerson();
            person.setAICoreId(getCommodityId());
            person.setPersonality(defaultPersonality);
            level = getBaseLevel();
            autoMult = getBaseAIPointsMult();
        } else {
            level = person.getStats().getLevel();
            autoMult = person.getMemoryWithoutUpdate().getFloat("$autoPointsMult");
        }

        float opad = 10f;
        Color text = Global.getSector().getPlayerFaction().getBaseUIColor();
        Color bg = Global.getSector().getPlayerFaction().getDarkUIColor();
        float w = (tooltip.getTextWidthOverride() <= 10f ? tooltip.getWidthSoFar() : tooltip.getTextWidthOverride()) - 5f;

        tooltip.addSectionHeading(Strings.Items.kCoreAdditionalInfo, text, bg, Alignment.MID, w+5f,20f);
        tooltip.addPara(body, opad, highlightColor, params);
        tooltip.beginTable(Global.getSector().getPlayerFaction(), 30f, Strings.Items.kCorePersonalityTableTitle1, w * 2f / 3f, Strings.Items.kCorePersonalityTableTitle2, w / 3f);
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.kCorePersonalityTableName1, Misc.getHighlightColor(), Utils.asInt(level));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.kCorePersonalityTableName2, Misc.getHighlightColor(), Utils.asFloatTwoDecimals(autoMult) + "x");
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.kCorePersonalityTableName3, Misc.getHighlightColor(), Misc.getPersonalityName(person));
        tooltip.addTable("", 0, opad);
        tooltip.addSpacer(opad);
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_beta_k_core.png";
    }
}
