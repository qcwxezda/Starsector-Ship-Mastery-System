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
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.MasteryUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

public class BaseKCorePlugin implements HullModFleetEffect, KCoreInterface {

    public static final String COPY_PERSONALITY_TAG = "sms_copy_player_personality";
    public static final String DEFAULT_PERSONALITY_ID = "aggressive";
    public static final String SHARED_KNOWLEDGE_ID = "sms_shared_knowledge";

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
    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        return initPerson(aiCoreId, factionId, getPortraitSpritePath(), getBaseAIPointsMult());
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
                int enhances = MasteryUtils.getEnhanceCount(fm.getHullSpec());
                float dpMult = Math.max(AmorphousCorePlugin.MIN_DP_MULT, AmorphousCorePlugin.BASE_DP_MULT - enhances * AmorphousCorePlugin.DP_MULT_PER_ENHANCE);
                var memory = captain.getMemoryWithoutUpdate();
                if (memory != null) {
                    memory.set("$autoPointsMult", dpMult);
                }
            }
        }
    }

    @Override
    public String getCommodityId() {
        throw new RuntimeException("commodity id not set");
    }

    @Override
    public int getBaseLevel() {
        return 0;
    }

    @Override
    public void setPersonSkills(MutableCharacterStatsAPI stats, String factionId) {}

    @Override
    public float getBaseAIPointsMult() {
        return 0f;
    }

    @Override
    public String getPortraitSpritePath() {
        throw new RuntimeException("portrait sprite path not set");
    }

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

    public String getPlayerPersonalityId() {
        var personalityPicker = Global.getSector().getPlayerFaction().getPersonalityPicker();
        if (personalityPicker != null && !personalityPicker.isEmpty()) {
            return personalityPicker.getItems().get(0);
        } else {
            return DEFAULT_PERSONALITY_ID;
        }
    }

    protected final void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip, String body, Color highlightColor, String... params) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        if (spec == null) return;

        int level;
        float autoMult;
        int baseLevel = getBaseLevel();
        float baseAutoMult = getBaseAIPointsMult();
        if (person == null) {
            String defaultPersonality = getPlayerPersonalityId();
            person = Global.getFactory().createPerson();
            person.setAICoreId(getCommodityId());
            person.setPersonality(defaultPersonality);
            level = baseLevel;
            autoMult = baseAutoMult;
        } else {
            level = person.getStats().getLevel();
            autoMult = person.getMemoryWithoutUpdate().getFloat("$autoPointsMult");
        }

        Color levelColor = level < baseLevel ? Misc.getNegativeHighlightColor() : level > baseLevel ? Misc.getPositiveHighlightColor() : Misc.getHighlightColor();
        Color autoMultColor = autoMult < baseAutoMult ? Misc.getPositiveHighlightColor() : autoMult > baseAutoMult ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor();

        float opad = 10f;
        Color text = Global.getSector().getPlayerFaction().getBaseUIColor();
        Color bg = Global.getSector().getPlayerFaction().getDarkUIColor();
        float w = (tooltip.getTextWidthOverride() <= 10f ? tooltip.getWidthSoFar() : tooltip.getTextWidthOverride()) - 5f;

        tooltip.addSectionHeading(Strings.Items.kCoreAdditionalInfo, text, bg, Alignment.MID, w+5f,20f);
        tooltip.addPara(body, opad, highlightColor, params);
        tooltip.beginTable(Global.getSector().getPlayerFaction(), 30f, Strings.Items.kCorePersonalityTableTitle1, w * 2f / 3f, Strings.Items.kCorePersonalityTableTitle2, w / 3f);
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.kCorePersonalityTableName1, levelColor, Utils.asInt(level));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.kCorePersonalityTableName2, autoMultColor, "×" + Utils.asFloatTwoDecimals(autoMult));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.kCorePersonalityTableName3, Misc.getHighlightColor(), Misc.getPersonalityName(person));
        tooltip.addTable("", 0, opad);
        tooltip.addSpacer(opad);
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
    }

    public void setPersonalityToPlayerDoctrine(PersonAPI person) {
        person.setPersonality(getPlayerPersonalityId());
    }

}
