package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.Strings;

public class SubknowledgeCorePlugin extends KnowledgeCorePlugin {

    public static final float DP_MULT = 2.5f;
    public static final int MAX_LEVEL = 4;

    @Override
    public float getBaseAIPointsMult() {
        return DP_MULT;
    }

    @Override
    public int getBaseLevel() {
        return MAX_LEVEL;
    }

    @Override
    public String getCommodityId() {
        return "sms_subknowledge_core";
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Strings.Items.knowledgeCorePersonalityText,
                Misc.getHighlightColor(),
                spec.getName());
    }

    @Override
    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_subknowledge_core.png";
    }

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(MAX_LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(AmorphousCorePlugin.UNIQUE_SKILL_ID, 2f);
    }
}
