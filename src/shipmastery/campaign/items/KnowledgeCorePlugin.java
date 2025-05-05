package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
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

public class KnowledgeCorePlugin extends BaseAICoreOfficerPluginImpl implements HullModFleetEffect, KnowledgeCoreInterface {

    public static final String COPY_PERSONALITY_TAG = "sms_copy_player_personality";
    public static final String DEFAULT_PERSONALITY_ID = "aggressive";
    public static final int MAX_LEVEL = 5;
    public static final float BASE_DP_MULT = 3.5f;
    public static final float DP_MULT_PER_MISSING_SKILL = 0.5f;
    public static final float MIN_DP_MULT = 1.5f;
    public static final String UNIQUE_SKILL_ID = "sms_shared_knowledge";

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

    @Override
    public String getCommodityId() {
        return "sms_knowledge_core";
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

    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        return initPerson(aiCoreId, factionId, getPortraitSpritePath(), getBaseAIPointsMult());
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Strings.Items.knowledgeCorePersonalityText + "\n\n" + Strings.Items.knowledgeCorePersonalityText2,
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

        tooltip.addSectionHeading(spec.getName(), text, bg, Alignment.MID, w+5f,20f);
        tooltip.addPara(body, opad, highlightColor, params);
        tooltip.beginTable(Global.getSector().getPlayerFaction(), 30f, Strings.Items.knowledgeCorePersonalityTableTitle1, w * 2f / 3f, Strings.Items.knowledgeCorePersonalityTableTitle2, w / 3f);
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.knowledgeCorePersonalityTableName1, Misc.getHighlightColor(), Utils.asInt(level));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.knowledgeCorePersonalityTableName2, Misc.getHighlightColor(), Utils.asFloatTwoDecimals(autoMult) + "x");
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.knowledgeCorePersonalityTableName3, Misc.getHighlightColor(), Misc.getPersonalityName(person));
        tooltip.addTable("", 0, opad);
        tooltip.addSpacer(opad);
    }

    public float getBaseAIPointsMult() {
        return BASE_DP_MULT;
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

            // Special behavior for knowledge (but not sub-knowledge) cores
            if ("sms_knowledge_core".equals(captain.getAICoreId())) {
                int count = (int) captain.getStats().getSkillsCopy()
                        .stream()
                        .filter((skill) -> skill.getLevel() > 0f && skill.getSkill().isCombatOfficerSkill())
                        .count();
                int diff = Math.max(0, captain.getStats().getLevel() - count);
                var memory = captain.getMemoryWithoutUpdate();
                if (memory != null) {
                    memory.set("$autoPointsMult", Math.max(MIN_DP_MULT, BASE_DP_MULT-DP_MULT_PER_MISSING_SKILL*diff));
                }
            }

            // Special behavior for amorphous cores
            if ("sms_amorphous_core".equals(captain.getAICoreId())) {
                boolean integrated = Misc.isUnremovable(fm.getCaptain());
                int level = Math.min(AmorphousCorePlugin.MAX_LEVEL, Math.round(AmorphousCorePlugin.MIN_LEVEL + AmorphousCorePlugin.LEVELS_PER_MASTERY_LEVEL * ShipMastery.getPlayerMasteryLevel(fm.getHullSpec())));
                if (integrated) {
                    level++;
                }
                captain.getStats().setLevel(level);
                int enhances = MasteryUtils.getEnhanceCount(fm.getHullSpec());
                float dpMult = Math.max(AmorphousCorePlugin.MIN_DP_MULT, AmorphousCorePlugin.MAX_DP_MULT - enhances * AmorphousCorePlugin.DP_MULT_PER_ENHANCE);
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

    public String getPlayerPersonalityId() {
        var personalityPicker = Global.getSector().getPlayerFaction().getPersonalityPicker();
        if (personalityPicker != null && !personalityPicker.isEmpty()) {
            return personalityPicker.getItems().get(0);
        } else {
            return DEFAULT_PERSONALITY_ID;
        }
    }

    public void setPersonalityToPlayerDoctrine(PersonAPI person) {
        person.setPersonality(getPlayerPersonalityId());
    }
}
