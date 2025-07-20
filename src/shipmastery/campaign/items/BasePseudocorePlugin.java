package shipmastery.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.backgrounds.Enlightened;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public abstract class BasePseudocorePlugin implements PseudocorePlugin {

    @Override
    public abstract String getCommodityId();
    public abstract int getBaseLevel();
    public abstract float getBaseAIPointsMult();

    @Override
    public final PersonAPI createPerson(String aiCoreId, String factionId, Random random) {
        return initPerson(aiCoreId, factionId);
    }

    public final int getLevel() {
        int base = getBaseLevel();
        if (Enlightened.isEnlightenedStart()) {
            base++;
        }
        return base;
    }

    @Override
    public List<String> getPrioritySkills() {
        List<String> ids = new ArrayList<>();
        ids.add(SHARED_KNOWLEDGE_ID);
        ids.add(Skills.HELMSMANSHIP);
        ids.add(Skills.TARGET_ANALYSIS);
        ids.add(Skills.IMPACT_MITIGATION);
        ids.add(Skills.FIELD_MODULATION);
        ids.add(Skills.GUNNERY_IMPLANTS);
        ids.add(Skills.COMBAT_ENDURANCE);
        ids.add(Skills.DAMAGE_CONTROL);
        return ids;
    }

    public final void setPersonSkills(MutableCharacterStatsAPI stats) {
        List<String> ids = getPrioritySkills();
        int level = getLevel();
        Set<String> assigned = new HashSet<>();
        for (int i = 0; i < Math.min(ids.size(), level); i++) {
            var id = ids.get(i);
            stats.setSkillLevel(id, 2f);
            assigned.add(id);
        }
        if (ids.size() < level) {
            int i = 0;
            for (String id : Utils.combatSkillIds) {
                if (assigned.contains(id) || i >= level-ids.size()) continue;
                stats.setSkillLevel(id, 2f);
                i++;
            }
        }
    }

    public float getEnlightenedAIMultIncrease() {
        return 0.5f;
    }

    public final float getAIPointsMult() {
        float base = getBaseAIPointsMult();
        if (Enlightened.isEnlightenedStart()) {
            base += getEnlightenedAIMultIncrease();
        }
        return base;
    }

    @Override
    public String getPortraitSpritePath() {
        throw new RuntimeException("portrait sprite path not set");
    }

    public final PersonAPI initPerson(String aiCoreId, String factionId) {
        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        person.getStats().setSkipRefresh(true);
        person.setName(new FullName(spec.getName(), "", FullName.Gender.ANY));
        var spritePath = getPortraitSpritePath();
        SpriteAPI sprite = Global.getSettings().getSprite(spritePath);
        if (sprite.getTextureId() <= 0) {
            try {
                Global.getSettings().loadTexture(spritePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        person.setPortraitSprite(spritePath);

        person.getStats().setLevel(getLevel());
        setPersonSkills(person.getStats());

        var aiPointsMult = getAIPointsMult();
        person.getMemoryWithoutUpdate().set("$autoPointsMult", aiPointsMult);
        person.setPersonality(DEFAULT_PERSONALITY_ID);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId(null);
        return person;
    }

    protected final void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip, Color highlightColor, String... params) {
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(getCommodityId());
        if (spec == null) return;

        int level;
        float autoMult;
        int defaultLevel = getLevel();
        float defaultAutoMult = getAIPointsMult();
        if (person == null) {
            String defaultPersonality = PseudocorePlugin.getPlayerPersonalityId();
            person = Global.getFactory().createPerson();
            person.setAICoreId(getCommodityId());
            person.setPersonality(defaultPersonality);
            level = defaultLevel;
            autoMult = defaultAutoMult;
        } else {
            level = person.getStats().getLevel();
            autoMult = person.getMemoryWithoutUpdate().getFloat("$autoPointsMult");
        }

        Color levelColor = level < defaultLevel ? Misc.getNegativeHighlightColor() : level > defaultLevel ? Misc.getPositiveHighlightColor() : Misc.getHighlightColor();
        Color autoMultColor = autoMult < defaultAutoMult ? Misc.getPositiveHighlightColor() : autoMult > defaultAutoMult ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor();

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

}
