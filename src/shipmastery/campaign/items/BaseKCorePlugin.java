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
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.CharacterStats;
import shipmastery.ShipMastery;
import shipmastery.util.MasteryUtils;

import java.io.IOException;
import java.util.Random;

public class BaseKCorePlugin implements HullModFleetEffect, KCoreInterface {

    public static final String COPY_PERSONALITY_TAG = "sms_copy_player_personality";
    public static final String DEFAULT_PERSONALITY_ID = "aggressive";

    public static final String UNIQUE_SKILL_ID = "sms_shared_knowledge";

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

            // Special behavior for knowledge (but not sub-knowledge) cores
            if ("sms_beta_k_core".equals(captain.getAICoreId())) {
                int count = (int) captain.getStats().getSkillsCopy()
                        .stream()
                        .filter((skill) -> skill.getLevel() > 0f && skill.getSkill().isCombatOfficerSkill())
                        .count();
                int diff = Math.max(0, captain.getStats().getLevel() - count);
                var memory = captain.getMemoryWithoutUpdate();
                if (memory != null) {
                    memory.set("$autoPointsMult", Math.max(BetaKCorePlugin.MIN_DP_MULT, BetaKCorePlugin.BASE_DP_MULT- BetaKCorePlugin.DP_MULT_PER_MISSING_SKILL*diff));
                }
            }

            // Special behavior for amorphous cores
            if ("sms_alpha_k_core".equals(captain.getAICoreId())) {
                boolean integrated = Misc.isUnremovable(fm.getCaptain());
                int level = Math.min(AlphaKCorePlugin.MAX_LEVEL, Math.round(AlphaKCorePlugin.MIN_LEVEL + AlphaKCorePlugin.LEVELS_PER_MASTERY_LEVEL * ShipMastery.getPlayerMasteryLevel(fm.getHullSpec())));
                if (integrated) {
                    level++;
                }
                captain.getStats().setLevel(level);
                int enhances = MasteryUtils.getEnhanceCount(fm.getHullSpec());
                float dpMult = Math.max(AlphaKCorePlugin.MIN_DP_MULT, AlphaKCorePlugin.MAX_DP_MULT - enhances * AlphaKCorePlugin.DP_MULT_PER_ENHANCE);
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

    @Override
    public void createPersonalitySection(PersonAPI person, TooltipMakerAPI tooltip) {}

    public void setPersonalityToPlayerDoctrine(PersonAPI person) {
        person.setPersonality(getPlayerPersonalityId());
    }

}
