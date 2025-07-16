package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.util.Strings;

public class AmorphousKnowledge {

    public static final String SHARED_KNOWLEDGE_NAME = Global.getSettings().getSkillSpec("sms_shared_knowledge").getName();
    public static final String CRYSTALLINE_KNOWLEDGE_NAME = Global.getSettings().getSkillSpec("sms_crystalline_knowledge").getName();
    public static final String WARPED_KNOWLEDGE_NAME = Global.getSettings().getSkillSpec("sms_warped_knowledge").getName();

    public static class Standard extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            new SharedKnowledge.Standard().apply(stats, hullSize, id, level);
            new WarpedKnowledge.Standard().apply(stats, hullSize, id, level);
            new CrystallineKnowledge.Standard().apply(stats, hullSize, id, level);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            new SharedKnowledge.Standard().unapply(stats, hullSize, id);
            new WarpedKnowledge.Standard().unapply(stats, hullSize, id);
            new CrystallineKnowledge.Standard().unapply(stats, hullSize, id);
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new WarpedKnowledge.Standard().applyEffectsAfterShipCreation(ship, id);
            new CrystallineKnowledge.Standard().applyEffectsAfterShipCreation(ship, id);
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new WarpedKnowledge.Standard().unapplyEffectsAfterShipCreation(ship, id);
            new CrystallineKnowledge.Standard().unapplyEffectsAfterShipCreation(ship, id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara(Strings.Skills.amorphousKnowledgeStandardEffect1, 0f, hc, hc, SHARED_KNOWLEDGE_NAME, WARPED_KNOWLEDGE_NAME, CRYSTALLINE_KNOWLEDGE_NAME);
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            new SharedKnowledge.Elite().apply(stats, hullSize, id, level);
            new WarpedKnowledge.Elite().apply(stats, hullSize, id, level);
            new CrystallineKnowledge.Elite().apply(stats, hullSize, id, level);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            new SharedKnowledge.Elite().unapply(stats, hullSize, id);
            new WarpedKnowledge.Elite().unapply(stats, hullSize, id);
            new CrystallineKnowledge.Elite().unapply(stats, hullSize, id);
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new SharedKnowledge.Elite().applyEffectsAfterShipCreation(ship, id);
            new WarpedKnowledge.Elite().applyEffectsAfterShipCreation(ship, id);
            new CrystallineKnowledge.Elite().applyEffectsAfterShipCreation(ship, id);
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new SharedKnowledge.Elite().unapplyEffectsAfterShipCreation(ship, id);
            new WarpedKnowledge.Elite().unapplyEffectsAfterShipCreation(ship, id);
            new CrystallineKnowledge.Elite().unapplyEffectsAfterShipCreation(ship, id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            initElite(stats, skill);
            info.addPara(Strings.Skills.amorphousKnowledgeEliteEffect1, 0f, hc, hc, SHARED_KNOWLEDGE_NAME, WARPED_KNOWLEDGE_NAME, CRYSTALLINE_KNOWLEDGE_NAME);
        }
    }

    public static class Hidden extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}
    }
}
