package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.AdvanceIfAliveListener;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.OverlayRenderer;
import shipmastery.util.CollisionUtils;
import shipmastery.util.MasteryUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.StackableBuffWithExpiry;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.awt.Color;

public class CrystallineKnowledge {

    public static boolean hasEliteCrystallineKnowledge(PersonAPI person) {
        if (person == null || person.getStats() == null) return false;
        return person.getStats().getSkillLevel("sms_crystalline_knowledge") >= 2f
                || person.getStats().getSkillLevel("sms_amorphous_knowledge") >= 2f;
    }

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

        public static final float EMP_DAMAGE = 400f;
        public static final float FIRE_RATE_REDUCTION_PER_STACK = 0.02f;
        public static final float MAX_FIRE_RATE_REDUCTION = 0.2f;
        public static final float[] MIN_DELAY_BETWEEN_SHOTS_SECONDS = new float[] {9f, 7f, 5f, 3f};
        public static final float DURATION_SECONDS = 10f;
        public static final float MAX_RANGE = 2500f;
        public static final String EXPLOSION_ID_KEY = "sms_IsCrystallineExplosion";
        public static final String DURATION_MOD = "sms_CrystallineDurationMod";
        public static final String STACK_STRENGTH_MOD = "sms_CrystallinePerStackMod";
        public static final String DEBUFF_CAP_MOD = "sms_CrystallineCapMod";

        public static class EliteEffectScript extends AdvanceIfAliveListener implements DamageTakenModifier, DamageDealtModifier {

            private final String id;
            private float cooldown = 0f;
            private final DamagingExplosionSpec explosionSpec;
            private final NegativeExplosionVisual.NEParams params;
            private final float masteryStrength;

            public static class DebuffScript extends StackableBuffWithExpiry {
                private final String id;
                private static final float FADE_OUT_TIME = 3f;

                public DebuffScript(ShipAPI ship, String id) {
                    super(ship, FADE_OUT_TIME);
                    this.id = id;
                    if (ship.getSpriteAPI() != null) {
                        var renderer = new OverlayRenderer(ship, new Color(0.3f, 0.3f, 1f)) {
                            @Override
                            public boolean isExpired() {
                                return !ship.hasListener(DebuffScript.this);
                            }

                            @Override
                            public float getOpacity() {
                                var opacity = 0.5f*strength/MAX_FIRE_RATE_REDUCTION;
                                if (durationRemaining <= 0f) {
                                    opacity *= 1f + durationRemaining/fadeOutTime;
                                }
                                opacity = MathUtils.clamp(opacity, 0f, 0.5f);
                                return opacity;
                            }
                        };
                        renderer.init(Global.getCombatEngine().addLayeredRenderingPlugin(renderer));
                    }
                }

                @Override
                public void unapplyEffects() {
                    var stats = ship.getMutableStats();
                    stats.getBallisticRoFMult().unmodify(id);
                    stats.getEnergyRoFMult().unmodify(id);
                    stats.getMissileRoFMult().unmodify(id);
                }

                @Override
                public void applyEffects(float fadeOutFrac) {
                    var stats = ship.getMutableStats();
                    var mod = strength * (1f - fadeOutFrac);
                    stats.getBallisticRoFMult().modifyMult(id, 1f-mod);
                    stats.getEnergyRoFMult().modifyMult(id, 1f-mod);
                    stats.getMissileRoFMult().modifyMult(id, 1f-mod);
                    Utils.maintainStatusForPlayerShip(
                            ship,
                            id,
                            "graphics/icons/tactical/broken.png",
                            Strings.Skills.crystallineKnowledgeEliteEffectDebuffTitle,
                            String.format(Strings.Skills.crystallineKnowledgeEliteEffectDebuffDesc, Utils.asPercentNoDecimal(mod)),
                            true);
                }
            }

            public EliteEffectScript(ShipAPI ship, String id) {
                super(ship);
                this.id = id;
                VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(ship.getVariant());
                masteryStrength = MasteryUtils.getModifiedMasteryEffectStrength(
                        ship.getFleetCommander(),
                        info.root == null ? ship.getHullSpec() : info.root.getHullSpec(),
                        1f);
                explosionSpec = new DamagingExplosionSpec(
                        0.1f,
                        80f,
                        40f,
                        EMP_DAMAGE / 10f,
                        EMP_DAMAGE / 10f,
                        CollisionClass.PROJECTILE_NO_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        3f, 3f, 1f, 0, Color.BLACK, Color.BLACK);
                explosionSpec.setShowGraphic(false);
                explosionSpec.setMinEMPDamage(EMP_DAMAGE);
                explosionSpec.setMaxEMPDamage(EMP_DAMAGE);
                explosionSpec.setUseDetailedExplosion(false);
                params = RiftCascadeMineExplosion.createStandardRiftParams(new Color(100, 100, 255), 25f);
                params.noiseMag = 3f;
                params.withNegativeParticles = false;
                params.fadeOut = 0.5f;
                params.fadeIn = 0.5f;
                params.hitGlowSizeMult = 0.4f;
                params.invertForDarkening = new Color(50, 50, 100);
                params.numRiftsToSpawn = 1;
                params.blackColor = Color.BLACK;
                params.underglow = new Color(50, 50, 100);
                params.noiseMult = 1f;
                params.noisePeriod = 0.2f;
                params.additiveBlend = true;
            }

            @Override
            public void advanceIfAlive(float amount) {
                cooldown -= amount;
            }

            @Override
            public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (param instanceof CombatEntityAPI entity && entity.getCustomData() != null && (Boolean) entity.getCustomData().getOrDefault(EXPLOSION_ID_KEY, Boolean.FALSE)) {
                    if (!(target instanceof ShipAPI targetShip)) return null;
                    StackableBuffWithExpiry.addStackToShip(
                            new DebuffScript(targetShip, id),
                            targetShip,
                            ship.getMutableStats().getDynamic().getMod(STACK_STRENGTH_MOD).computeEffective(FIRE_RATE_REDUCTION_PER_STACK),
                            ship.getMutableStats().getDynamic().getMod(DEBUFF_CAP_MOD).computeEffective(MAX_FIRE_RATE_REDUCTION),
                            ship.getMutableStats().getDynamic().getMod(DURATION_MOD).computeEffective(DURATION_SECONDS));
                }
                return null;
            }

            @Override
            public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (cooldown <= 0f) {
                    if (damage.getStats() == null || !(damage.getStats().getEntity() instanceof ShipAPI source)) return null;
                    if (!Global.getCombatEngine().isShipAlive(source)) return null;
                    if (source.getOwner() == ship.getOwner()) return null;

                    var amount = damage.getDamage();
                    if (damage.isDps()) amount *= 0.1f;
                    var chance = 1f - Math.pow(1.5f, -amount/100f);
                    if (Misc.random.nextFloat() > chance) return null;

                    var params = new EmpArcEntityAPI.EmpArcParams();
                    params.segmentLengthMult = 4f;
                    params.glowSizeMult = 1f;
                    params.flickerRateMult = 0.25f + (float) Math.random() * 0.1f;
                    var data = CollisionUtils.rayCollisionCheckEntity(point, source.getLocation(), source);
                    if (data.one == null || MathUtils.dist(data.one, point) > MAX_RANGE) return null;
                    var arc = Global.getCombatEngine().spawnEmpArcVisual(
                            point,
                            ship,
                            data.one,
                            source,
                            50f,
                            new Color(100, 100, 255),
                            Color.WHITE,
                            params);
                    arc.setRenderGlowAtStart(false);
                    arc.setSingleFlickerMode(true);

                    cooldown = MIN_DELAY_BETWEEN_SHOTS_SECONDS[Utils.hullSizeToInt(ship.getHullSize())] / masteryStrength;
                    float fadeIn = (float) ship.getCustomData().getOrDefault(HiddenEffectScript.EFFECT_FADE_IN_KEY, 0f);
                    cooldown *= MathUtils.lerp(1f, 0.333f, fadeIn);
                    cooldown *= MathUtils.randBetween(0.8f, 1.2f);

                    CombatDeferredActionPlugin.performLater(() -> {
                        var explosion = Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, data.one, false);
                        explosion.setCustomData(EXPLOSION_ID_KEY, true);
                        RiftCascadeMineExplosion.spawnStandardRift(explosion, this.params);
                        Global.getSoundPlayer().playSound("riftcascade_rift", 1.5f, 0.45f, point, new Vector2f());
                    }, 0.1f);
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
        public static class EffectScript extends HiddenEffectScript {
            public EffectScript(ShipAPI ship, String id, Provider plugin) {
                super(ship, id, new Color(100, 150, 225), plugin);
            }

            @Override
            protected void applyEffectsToShip(ShipAPI ship, float effectLevel) {
                effectLevel = Math.min(effectLevel, 0.9f);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - effectLevel);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - effectLevel);
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f - effectLevel);
                ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 1f - effectLevel);
            }

            @Override
            protected void unapplyEffectsToShip(ShipAPI ship) {
                ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                ship.getMutableStats().getEmpDamageTakenMult().unmodify(id);
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
