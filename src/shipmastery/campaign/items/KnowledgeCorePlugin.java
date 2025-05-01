package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.PersonalityAPI;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CharacterStats;
import shipmastery.ShipMastery;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

import static shipmastery.campaign.items.AmorphousCorePlugin.*;

public class KnowledgeCorePlugin extends BaseAICoreOfficerPluginImpl implements HullModFleetEffect {

    public static final String COPY_PERSONALITY_TAG = "sms_copy_player_personality";
    public static final String DEFAULT_PERSONALITY_ID = "aggressive";
    public static final int MAX_LEVEL = 6;
    public static final float DP_MULT = 3.5f;

    public PersonAPI initPerson(String aiCoreId, String factionId, String spritePath, float aiPointsMult) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        person.getStats().setSkipRefresh(true);
        person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
        SpriteAPI sprite = Global.getSettings().getSprite(spritePath);
        if (sprite.getTextureId() <= 0) {
            try {
                Global.getSettings().loadTexture(spritePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        person.setPortraitSprite(spritePath);

        setPersonSkills(person.getStats(), factionId);

        person.getMemoryWithoutUpdate().set("$autoPointsMult", aiPointsMult);
        person.setPersonality(DEFAULT_PERSONALITY_ID);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId(null);
        return person;
    }

    public int getMaxLevel() {
        return MAX_LEVEL;
    }

    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {
        stats.setLevel(MAX_LEVEL);
        stats.setSkillLevel(Skills.HELMSMANSHIP, 2f);
        stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f);
        stats.setSkillLevel(Skills.IMPACT_MITIGATION, 2f);
        stats.setSkillLevel(Skills.FIELD_MODULATION, 2f);
        stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2f);
        stats.setSkillLevel(AmorphousCorePlugin.UNIQUE_SKILL_ID, 2f);
    }

    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        return initPerson(aiCoreId, factionId, getPortraitSpritePath(), getAIPointsMult());
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(person.getAICoreId());
        PersonalityAPI personality = person.getPersonalityAPI();
        if (personality == null) personality = Global.getSettings().getPersonaltySpec(DEFAULT_PERSONALITY_ID);
        createPersonalitySection(
                tooltip,
                Strings.Items.knowledgeCorePersonalityHeading,
                String.format(Strings.Items.knowledgeCorePersonalityText, spec.getName()),
                getMaxLevel(),
                getAIPointsMult(),
                personality.getDisplayName());
    }

    public static void createPersonalitySection(TooltipMakerAPI tooltip, String header, String bodyFmt, int maxLevel, float autoPtsMult, String personalityName) {
        float opad = 10f;
        Color text = Global.getSector().getPlayerFaction().getBaseUIColor();
        Color bg = Global.getSector().getPlayerFaction().getDarkUIColor();
        float w = (tooltip.getTextWidthOverride() <= 10f ? tooltip.getWidthSoFar() : tooltip.getTextWidthOverride()) - 5f;

        tooltip.addSectionHeading(header, text, bg, Alignment.MID, w+5f,20f);
        tooltip.addPara(bodyFmt, opad);
        tooltip.beginTable(Global.getSector().getPlayerFaction(), 30f, Strings.Items.knowledgeCorePersonalityTableTitle1, w * 2f / 3f, Strings.Items.knowledgeCorePersonalityTableTitle2, w / 3f);
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.knowledgeCorePersonalityTableName1, Misc.getHighlightColor(), Utils.asInt(maxLevel));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.knowledgeCorePersonalityTableName2, Misc.getHighlightColor(), Utils.asFloatTwoDecimals(autoPtsMult) + "x");
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.knowledgeCorePersonalityTableName3, Misc.getHighlightColor(), personalityName);
        tooltip.addTable("", 0, 2f*opad);
    }

    public float getAIPointsMult() {
        return DP_MULT;
    }

    public String getPortraitSpritePath() {
        return "graphics/portraits/sms_portrait_knowledge_core.png";
    }

    @Override
    public void advanceInCampaign(CampaignFleetAPI fleet) {}

    @Override
    public boolean withAdvanceInCampaign() {
        return false;
    }

    @Override
    public boolean withOnFleetSync() {
        return true;
    }

    @Override
    public void onFleetSync(CampaignFleetAPI fleet) {
        if (!fleet.isPlayerFleet()) return;
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            PersonAPI captain = fm.getCaptain();
            if (captain == null || captain.getAICoreId() == null) continue;
            var spec = Global.getSettings().getCommoditySpec(captain.getAICoreId());
            if (spec.hasTag(COPY_PERSONALITY_TAG)) {
                setPersonalityToPlayerDoctrine(captain);
            }

            // Special behavior for amorphous cores
            if ("sms_amorphous_core".equals(captain.getAICoreId())) {
                boolean integrated = Misc.isUnremovable(fm.getCaptain());
                int level = Math.min(MAX_LEVEL, Math.round(MIN_LEVEL + LEVELS_PER_MASTERY_LEVEL * ShipMastery.getPlayerMasteryLevel(fm.getHullSpec())));
                if (integrated) {
                    level++;
                }
                captain.getStats().setLevel(level);
                int enhances = MasteryUtils.getEnhanceCount(fm.getHullSpec());
                float dpMult = Math.max(MIN_DP_MULT, MAX_DP_MULT - enhances * DP_MULT_PER_ENHANCE);
                var memory = captain.getMemoryWithoutUpdate();
                if (memory != null) {
                    memory.set("$autoPointsMult", dpMult);
                }

                // Remove skills if over the cap
                var itr = ((CharacterStats) captain.getStats()).getSkills().iterator();
                int count = 0;
                while (itr.hasNext()) {
                    CharacterStats.SkillLevel skill = itr.next();
                    if (!skill.getSkill().isCombatOfficerSkill()) continue;
                    if (skill.getLevel() <= 0f) continue;
                    if (UNIQUE_SKILL_ID.equals(skill.getSkill().getId())) continue;
                    count++;
                    if (count > level) {
                        itr.remove();
                    }
                }
            }
        }
    }

    public void setPersonalityToPlayerDoctrine(PersonAPI person) {
        var personalityPicker = Global.getSector().getPlayerFaction().getPersonalityPicker();
        if (personalityPicker != null && !personalityPicker.isEmpty()) {
            person.setPersonality(personalityPicker.getItems().get(0));
        } else {
            person.setPersonality(DEFAULT_PERSONALITY_ID);
        }
    }
}
