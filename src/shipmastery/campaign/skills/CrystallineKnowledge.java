package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.AdvanceIfAliveListener;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;

public class CrystallineKnowledge {
    public static class Standard extends SkillEffectDescriptionWIthNegativeHighlight implements AfterShipCreationSkillEffect {

        public static final float MAX_DR_SHIELDS = 0.25f;
        public static final float SHIELD_THRESHOLD_MIN = 50f;
        public static final float SHIELD_THRESHOLD_MAX = 500f;
        public static final float MAX_DR_HULL_ARMOR = 0.25f;
        public static final float ARMOR_THRESHOLD_MIN = 100f;
        public static final float ARMOR_THRESHOLD_MAX = 1000f;
        public static final float BEAM_DR = 0.2f;
        public static final float FIRE_RATE_REDUCTION = 0.15f;

        public static class StandardEffectScript implements DamageTakenModifier {
            private final String id;
            public StandardEffectScript(String id) {
                this.id = id;
            }

            @Override
            public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (damage.isDps()) return null;
                float dam = damage.getDamage();
                float t, dr;
                if (shieldHit) {
                    dam = Math.min(Math.max(dam, SHIELD_THRESHOLD_MIN), SHIELD_THRESHOLD_MAX);
                    t = 1f - (dam - SHIELD_THRESHOLD_MIN) / (SHIELD_THRESHOLD_MAX - SHIELD_THRESHOLD_MIN);
                    dr = MathUtils.lerp(0f, MAX_DR_SHIELDS, t);
                } else {
                    dam = Math.min(Math.max(dam,  ARMOR_THRESHOLD_MIN), ARMOR_THRESHOLD_MAX);
                    t = (dam - ARMOR_THRESHOLD_MIN) /  (ARMOR_THRESHOLD_MAX - ARMOR_THRESHOLD_MIN);
                    dr = MathUtils.lerp(0, MAX_DR_HULL_ARMOR, t);
                }

                if (dr <= 0f) return null;
                damage.getModifier().modifyMult(id, 1f - dr);
                return id;
            }
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getMissileRoFMult().modifyMult(id, 1f-FIRE_RATE_REDUCTION);
            stats.getBallisticRoFMult().modifyMult(id, 1f-FIRE_RATE_REDUCTION);
            stats.getEnergyRoFMult().modifyMult(id, 1f-FIRE_RATE_REDUCTION);
            stats.getBeamDamageTakenMult().modifyMult(id, 1f - BEAM_DR);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getMissileRoFMult().unmodify(id);
            stats.getBallisticRoFMult().unmodify(id);
            stats.getEnergyRoFMult().unmodify(id);
            stats.getBeamDamageTakenMult().unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara(Strings.Skills.crystallineKnowledgeStandardEffect1, 0f, hc, hc, Utils.asPercent(MAX_DR_SHIELDS));
            info.addPara(Strings.Skills.crystallineKnowledgeStandardEffect2, 0f, tc, hc, Utils.asInt(SHIELD_THRESHOLD_MIN), Utils.asInt(SHIELD_THRESHOLD_MAX));
            info.addPara(Strings.Skills.crystallineKnowledgeStandardEffect3, 0f, hc, hc, Utils.asPercent(MAX_DR_HULL_ARMOR));
            info.addPara(Strings.Skills.crystallineKnowledgeStandardEffect4, 0f, tc, hc, Utils.asInt(ARMOR_THRESHOLD_MAX), Utils.asInt(ARMOR_THRESHOLD_MIN));
            info.addPara(Strings.Skills.crystallineKnowledgeStandardEffect5, 0f, hc, hc, Utils.asPercent(BEAM_DR));
            info.addPara(Strings.Skills.crystallineKnowledgeStandardEffect6, 0f, nhc, nhc, Utils.asPercent(FIRE_RATE_REDUCTION));
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new StandardEffectScript(id));
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(StandardEffectScript.class);
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        public static final float EMP_DAMAGE = 500f;
        public static final float FIRE_RATE_REDUCTION_PER_STACK = 0.02f;
        public static final float MAX_FIRE_RATE_REDUCTION = 0.2f;
        public static final float[] MIN_DELAY_BETWEEN_SHOTS_SECONDS = new float[] {8f, 6f, 4f, 2f};
        public static final float DURATION_SECONDS = 10f;

        public static class EliteEffectScript extends AdvanceIfAliveListener implements DamageTakenModifier {

            private final String id;
            private float cooldown = 0f;

            public EliteEffectScript(ShipAPI ship, String id) {
                super(ship);
                this.id = id;
            }

            @Override
            public void advanceIfAlive(float amount) {
                cooldown -= amount;
            }

            @Override
            public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (damage.getStats() == null || !(damage.getStats().getEntity() instanceof ShipAPI source)) return null;
                if (cooldown <= 0f) {
                    cooldown = MIN_DELAY_BETWEEN_SHOTS_SECONDS[Utils.hullSizeToInt(ship.getHullSize())];
                    var params = new EmpArcEntityAPI.EmpArcParams();
                    params.segmentLengthMult = 4f;
                    params.glowSizeMult = 4f;
                    params.flickerRateMult = 0.5f + (float) Math.random() * 0.5f;
                    var arc = Global.getCombatEngine().spawnEmpArcVisual(
                            point,
                            ship,
                            source.getLocation(),
                            source,
                            50f,
                            new Color(100, 100, 255),
                            Color.WHITE,
                            params);
                    arc.setRenderGlowAtStart(false);
                    arc.setSingleFlickerMode(true);
                }
                return null;
            }
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.addListener(new EliteEffectScript(ship, id));
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(EliteEffectScript.class);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            initElite(stats, skill);
            info.addPara(Strings.Skills.crystallineKnowledgeEliteEffect1, 0f, hc, hc,
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[0]),
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[1]),
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[2]),
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[3]),
                    Utils.asInt(EMP_DAMAGE),
                    Utils.asPercent(FIRE_RATE_REDUCTION_PER_STACK),
                    Utils.asFloatOneDecimal(DURATION_SECONDS));
            info.addPara(Strings.Skills.crystallineKnowledgeEliteEffect2, 0f, tc, hc, Utils.asPercent(MAX_FIRE_RATE_REDUCTION));
            info.addPara(Strings.Skills.crystallineKnowledgeEliteEffect3, tc, 0f);
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
