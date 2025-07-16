package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CampaignEngine;
import shipmastery.ShipMastery;
import shipmastery.campaign.listeners.CoreTabListener;
import shipmastery.campaign.listeners.PlayerFleetSyncListener;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BasePseudocorePlugin implements PlayerFleetSyncListener, PseudocoreInterface, CoreTabListener {

    public static final String COPY_PERSONALITY_TAG = "sms_copy_player_personality";
    public static final String DEFAULT_PERSONALITY_ID = "aggressive";
    public static final String SHARED_KNOWLEDGE_ID = "sms_shared_knowledge";
    public static final String CRYSTALLINE_KNOWLEDGE_ID = "sms_crystalline_knowledge";
    public static final String WARPED_KNOWLEDGE_ID = "sms_warped_knowledge";
    public static final String AMORPHOUS_KNOWLEDGE_ID = "sms_amorphous_knowledge";
    public static final String IS_PSEUDOCORE_TAG = "sms_pseudocore";

    @Override
    public PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        return initPerson(aiCoreId, factionId, getPortraitSpritePath(), getBaseAIPointsMult());
    }

    @Override
    public void onPlayerFleetSync() {
        var fleet = Global.getSector().getPlayerFleet();
        Map<String, AICoreOfficerPlugin> plugins = new HashMap<>();
        for (FleetMemberAPI fm : fleet.getFleetData().getMembersListCopy()) {
            PersonAPI captain = fm.getCaptain();
            String id = captain == null ? null : captain.getAICoreId();
            if (id == null) continue;
            var spec = Global.getSettings().getCommoditySpec(id);
            if (spec != null) {
                if (spec.hasTag(COPY_PERSONALITY_TAG)) {
                    setPersonalityToPlayerDoctrine(captain);
                }
                if (spec.hasTag(IS_PSEUDOCORE_TAG)) {
                    float ratio = fm.getUnmodifiedDeploymentPointsCost() / fm.getDeploymentPointsCost();
                    var plugin = plugins.computeIfAbsent(id, k -> CampaignEngine.getInstance().getModAndPluginData().pickAICoreOfficerPlugin(k));
                    var memory = captain.getMemoryWithoutUpdate();
                    if (memory != null && plugin instanceof BasePseudocorePlugin kPlugin) {
                        float baseMult = kPlugin.getBaseAIPointsMult();
                        // Special behavior for amorphous cores
                        if ("sms_amorphous_pseudocore".equals(id)) {
                            int points = (int) ShipMastery.getPlayerMasteryPoints(fm.getHullSpec());
                            int groups = (int) (points / AmorphousPseudocorePlugin.MP_PER_GROUP);
                            baseMult = Math.max(1f, baseMult - groups*AmorphousPseudocorePlugin.DP_MULT_PER_MP_GROUP);
                        }
                        memory.set("$autoPointsMult", baseMult * ratio);
                    }
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

    protected final void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip, Color highlightColor, String... params) {
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

        tooltip.addSectionHeading(Strings.Items.pseudocoreAdditionalInfo, text, bg, Alignment.MID, w+5f,20f);
        tooltip.addPara(Strings.Items.pseudocorePersonalityText, opad, highlightColor, params);
        tooltip.beginTable(Global.getSector().getPlayerFaction(), 30f, Strings.Items.pseudocorePersonalityTableTitle1, w * 2f / 3f, Strings.Items.pseudocorePersonalityTableTitle2, w / 3f);
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.pseudocorePersonalityTableName1, levelColor, Utils.asInt(level));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.pseudocorePersonalityTableName2, autoMultColor, "×" + Utils.asFloatTwoDecimals(autoMult));
        tooltip.addRowWithGlow(Misc.getTextColor(), Strings.Items.pseudocorePersonalityTableName3, Misc.getHighlightColor(), Misc.getPersonalityName(person));
        tooltip.addTable("", 0, opad);
        tooltip.addPara(Strings.Items.pseudocorePersonalityText2, opad);
    }

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        createPersonalitySection(
                person,
                tooltip,
                Misc.getHighlightColor(),
                spec.getName());
    }

    public void setPersonalityToPlayerDoctrine(PersonAPI person) {
        person.setPersonality(getPlayerPersonalityId());
    }

    @Override
    public void onCoreTabOpened(CoreUITabId id) {}

    @Override
    public void onCoreUIDismissed() {
        onPlayerFleetSync();
    }
}
