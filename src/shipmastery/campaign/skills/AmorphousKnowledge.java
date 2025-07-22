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

import java.awt.Color;

public class AmorphousKnowledge {

    public static final String SHARED_KNOWLEDGE_NAME = Global.getSettings().getSkillSpec("sms_shared_knowledge").getName();
    public static final String CRYSTALLINE_KNOWLEDGE_NAME = Global.getSettings().getSkillSpec("sms_crystalline_knowledge").getName();
    public static final String WARPED_KNOWLEDGE_NAME = Global.getSettings().getSkillSpec("sms_warped_knowledge").getName();

    public static class Standard extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            new SharedKnowledge.Standard().apply(stats, hullSize, id + "_1", level);
            new WarpedKnowledge.Standard().apply(stats, hullSize, id + "_2", level);
            new CrystallineKnowledge.Standard().apply(stats, hullSize, id + "_3", level);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            new SharedKnowledge.Standard().unapply(stats, hullSize, id + "_1");
            new WarpedKnowledge.Standard().unapply(stats, hullSize, id + "_2");
            new CrystallineKnowledge.Standard().unapply(stats, hullSize, id + "_3");
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new WarpedKnowledge.Standard().applyEffectsAfterShipCreation(ship, id + "_2");
            new CrystallineKnowledge.Standard().applyEffectsAfterShipCreation(ship, id + "_3");
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new WarpedKnowledge.Standard().unapplyEffectsAfterShipCreation(ship, id + "_2");
            new CrystallineKnowledge.Standard().unapplyEffectsAfterShipCreation(ship, id +"_3");
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
            new SharedKnowledge.Elite().apply(stats, hullSize, id + "_1", level);
            new WarpedKnowledge.Elite().apply(stats, hullSize, id + "_2", level);
            new CrystallineKnowledge.Elite().apply(stats, hullSize, id + "_3", level);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            new SharedKnowledge.Elite().unapply(stats, hullSize, id + "_1");
            new WarpedKnowledge.Elite().unapply(stats, hullSize, id + "_2");
            new CrystallineKnowledge.Elite().unapply(stats, hullSize, id + "_3");
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new SharedKnowledge.Elite().applyEffectsAfterShipCreation(ship, id + "_1");
            new WarpedKnowledge.Elite().applyEffectsAfterShipCreation(ship, id + "_2");
            new CrystallineKnowledge.Elite().applyEffectsAfterShipCreation(ship, id + "_3");
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            new SharedKnowledge.Elite().unapplyEffectsAfterShipCreation(ship, id + "_1");
            new WarpedKnowledge.Elite().unapplyEffectsAfterShipCreation(ship, id + "_2");
            new CrystallineKnowledge.Elite().unapplyEffectsAfterShipCreation(ship, id + "_3");
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            initElite(stats, skill);
            info.addPara(Strings.Skills.amorphousKnowledgeEliteEffect1, 0f, hc, hc, SHARED_KNOWLEDGE_NAME, WARPED_KNOWLEDGE_NAME, CRYSTALLINE_KNOWLEDGE_NAME);
        }
    }

    public static class Hidden extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public static class EffectScript extends HiddenEffectScript {
            public EffectScript(ShipAPI ship, String id, Provider plugin) {
                super(ship, id, new Color(200, 150, 200), plugin);
            }

            @Override
            protected void applyEffectsToShip(ShipAPI ship, float effectLevel) {
                effectLevel = Math.min(effectLevel, 0.9f);
                var stats = ship.getMutableStats();
                stats.getHullDamageTakenMult().modifyMult(id, 1f - effectLevel);
                stats.getArmorDamageTakenMult().modifyMult(id, 1f - effectLevel);
                stats.getShieldDamageTakenMult().modifyMult(id, 1f - effectLevel);
                stats.getEmpDamageTakenMult().modifyMult(id, 1f - effectLevel);
                stats.getBallisticWeaponDamageMult().modifyPercent(id, 100f * effectLevel);
                stats.getEnergyWeaponDamageMult().modifyPercent(id, 100f * effectLevel);
                stats.getMissileWeaponDamageMult().modifyPercent(id, 100f * effectLevel);
                stats.getTimeMult().modifyMult(id, 1f + effectLevel);
            }

            @Override
            protected void unapplyEffectsToShip(ShipAPI ship) {
                var stats = ship.getMutableStats();
                stats.getHullDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().unmodify(id);
                stats.getShieldDamageTakenMult().unmodify(id);
                stats.getEmpDamageTakenMult().unmodify(id);
                stats.getBallisticWeaponDamageMult().unmodify(id);
                stats.getEnergyWeaponDamageMult().unmodify(id);
                stats.getMissileWeaponDamageMult().unmodify(id);
                stats.getTimeMult().unmodify(id);
            }
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            var p = SharedKnowledge.getHiddenEffectPlugin(ship);
            if (p != null) {
                ship.addListener(new EffectScript(ship, id, p));
            }
        }
        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(EffectScript.class);
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}
    }
}