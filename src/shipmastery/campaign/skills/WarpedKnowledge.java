package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.AdvanceIfAliveListener;
import shipmastery.util.MasteryUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.awt.Color;

public class WarpedKnowledge {
    public static class Standard extends SkillEffectDescriptionWIthNegativeHighlight implements AfterShipCreationSkillEffect {

        public static final float MAX_DAMAGE_SHIELDS = 0.25f;
        public static final float SHIELD_THRESHOLD_MIN = 0.5f;
        public static final float SHIELD_THRESHOLD_MAX = 1.2f;
        public static final float MAX_DAMAGE_HULL_ARMOR = 0.25f;
        public static final float ARMOR_THRESHOLD_MIN = 50f;
        public static final float ARMOR_THRESHOLD_MAX = 1000f;
        public static final float DAMAGE_TAKEN_MOD = 0.15f;

        public static class StandardEffectScript implements DamageDealtModifier {
            private final String id;
            public StandardEffectScript(String id) {
                this.id = id;
            }

            @Override
            public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (!(target instanceof ShipAPI ship)) return null;
                if (shieldHit && ship.getShield() != null) {
                    float eff = ship.getShield().getFluxPerPointOfDamage();
                    eff = Math.min(Math.max(eff, SHIELD_THRESHOLD_MIN), SHIELD_THRESHOLD_MAX);
                    float t = 1f - (eff - SHIELD_THRESHOLD_MIN) / (SHIELD_THRESHOLD_MAX - SHIELD_THRESHOLD_MIN);
                    float bonus = MathUtils.lerp(0f, MAX_DAMAGE_SHIELDS, t);
                    if (bonus <= 0f) return null;
                    damage.getModifier().modifyPercent(id,  100f*bonus);
                    return id;
                } else {
                    int[] xy = ship.getArmorGrid().getCellAtLocation(point);
                    if (xy == null) return null;
                    float armor = ship.getMutableStats().getEffectiveArmorBonus()
                            .computeEffective(15f * ship.getArmorGrid().getArmorValue(xy[0], xy[1]));
                    armor = Math.max(armor, ship.getArmorGrid().getArmorRating() * ship.getMutableStats().getMinArmorFraction().getModifiedValue());
                    armor = Math.min(Math.max(armor, ARMOR_THRESHOLD_MIN), ARMOR_THRESHOLD_MAX);
                    float t = (armor - ARMOR_THRESHOLD_MIN) / (ARMOR_THRESHOLD_MAX - ARMOR_THRESHOLD_MIN);
                    float bonus = MathUtils.lerp(0f, MAX_DAMAGE_HULL_ARMOR, t);
                    if (bonus <= 0f) return null;
                    damage.getModifier().modifyPercent(id, 100f*bonus);
                    return id;
                }
            }
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getEmpDamageTakenMult().modifyPercent(id, 100f * DAMAGE_TAKEN_MOD);
            stats.getHullDamageTakenMult().modifyPercent(id, 100f * DAMAGE_TAKEN_MOD);
            stats.getArmorDamageTakenMult().modifyPercent(id, 100f * DAMAGE_TAKEN_MOD);
            stats.getShieldDamageTakenMult().modifyPercent(id, 100f * DAMAGE_TAKEN_MOD);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getEmpDamageTakenMult().unmodify(id);
            stats.getHullDamageTakenMult().unmodify(id);
            stats.getArmorDamageTakenMult().unmodify(id);
            stats.getShieldDamageTakenMult().unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara(Strings.Skills.warpedKnowledgeStandardEffect1, 0f, hc, hc, Utils.asPercent(MAX_DAMAGE_SHIELDS));
            info.addPara(Strings.Skills.warpedKnowledgeStandardEffect2, 0f, tc, hc, Utils.asFloatTwoDecimals(SHIELD_THRESHOLD_MIN), Utils.asFloatTwoDecimals(SHIELD_THRESHOLD_MAX));
            info.addPara(Strings.Skills.warpedKnowledgeStandardEffect3, 0f, hc, hc, Utils.asPercent(MAX_DAMAGE_HULL_ARMOR));
            info.addPara(Strings.Skills.warpedKnowledgeStandardEffect4, 0f, tc, hc, Utils.asInt(ARMOR_THRESHOLD_MAX), Utils.asInt(ARMOR_THRESHOLD_MIN));
            info.addPara(Strings.Skills.warpedKnowledgeStandardEffect5, 0f, nhc, nhc, Utils.asPercent(DAMAGE_TAKEN_MOD));
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

        public static final float EXPLOSION_DAMAGE = 500f;
        public static final float DAMAGE_TAKEN_PER_STACK = 0.02f;
        public static final float MAX_DAMAGE_TAKEN = 0.2f;
        public static final float[] MIN_DELAY_BETWEEN_SHOTS_SECONDS = new float[] {8f, 6f, 4f, 2f};
        public static final float DURATION_SECONDS = 10f;
        public static final String EXPLOSION_ID_KEY = "sms_IsWarpedExplosion";

        public static class DebuffScript extends AdvanceIfAliveListener {

            public class DebuffRenderer extends BaseCombatLayeredRenderingPlugin {
                public DebuffRenderer() {
                    super();
                    layer = CombatEngineLayers.ABOVE_SHIPS_LAYER;
                    entity = ship;
                }

                @Override
                public float getRenderRadius() {
                    return 600f;
                }

                @Override
                public void render(CombatEngineLayers layer, ViewportAPI viewport) {
                    var origColor = ship.getSpriteAPI().getColor();
                    var origBlendSrc = ship.getSpriteAPI().getBlendSrc();
                    var origBlendDst = ship.getSpriteAPI().getBlendDest();
                    var opacity = 0.5f*strength/MAX_DAMAGE_TAKEN;
                    if (durationRemaining <= 0f) {
                        opacity *= 1f + durationRemaining/FADE_OUT_TIME;
                    }
                    opacity = MathUtils.clamp(opacity, 0f, 0.5f);
                    ship.getSpriteAPI().setColor(new Color(1f, 0.3f, 0.3f, opacity));
                    ship.getSpriteAPI().setAdditiveBlend();
                    ship.getSpriteAPI().renderAtCenter(ship.getLocation().x , ship.getLocation().y);
                    ship.getSpriteAPI().setBlendFunc(origBlendSrc, origBlendDst);
                    ship.getSpriteAPI().setColor(origColor);
                }

                @Override
                public boolean isExpired() {
                    return !ship.hasListener(DebuffScript.this);
                }
            }

            private final String id;
            private float strength = 0f;
            private float durationRemaining = 0f;
            private static final float FADE_OUT_TIME = 3f;

            public DebuffScript(ShipAPI ship, String id) {
                super(ship);
                this.id = id;
                if (ship.getSpriteAPI() != null) {
                    Global.getCombatEngine()
                            .addLayeredRenderingPlugin(new DebuffRenderer())
                            .getLocation()
                            .set(ship.getLocation());
                }
            }

            @Override
            public void advanceIfAlive(float amount) {
                durationRemaining -= amount;
                if (durationRemaining <= -FADE_OUT_TIME) {
                    var stats = ship.getMutableStats();
                    stats.getShieldDamageTakenMult().unmodify(id);
                    stats.getArmorDamageTakenMult().unmodify(id);
                    stats.getHullDamageTakenMult().unmodify(id);
                    stats.getEmpDamageTakenMult().unmodify(id);
                    ship.removeListener(this);
                    return;
                }

                float effectStrength = 1f;
                if (durationRemaining < 0f) {
                    effectStrength += durationRemaining / FADE_OUT_TIME;
                }

                var stats = ship.getMutableStats();
                var mod = 100f * strength * effectStrength;
                stats.getShieldDamageTakenMult().modifyPercent(id, mod);
                stats.getArmorDamageTakenMult().modifyPercent(id, mod);
                stats.getHullDamageTakenMult().modifyPercent(id, mod);
                stats.getEmpDamageTakenMult().modifyPercent(id, mod);
                Utils.maintainStatusForPlayerShip(
                        ship,
                        id,
                        "graphics/icons/nascent_well2.png",
                        Strings.Skills.warpedKnowledgeEliteEffectDebuffTitle,
                        String.format(Strings.Skills.warpedKnowledgeEliteEffectDebuffDesc, Utils.asPercentNoDecimal(mod/100f)),
                        true);
            }

            public void addStack(float stackStrength, float maxStrength, float durationToKeep) {
                if (strength > maxStrength) return;
                strength = Math.min(maxStrength, strength + stackStrength);
                durationRemaining = Math.max(durationToKeep, durationRemaining);
            }
        }

        public static class EliteEffectScript extends AdvanceIfAliveListener implements DamageDealtModifier {

            private final String id;
            private float cooldown = 0f;
            private final DamagingExplosionSpec explosionSpec;
            private final NegativeExplosionVisual.NEParams params;
            private final float masteryStrength;

            public EliteEffectScript(ShipAPI ship, String id) {
                super(ship);
                this.id = id;
                explosionSpec = new DamagingExplosionSpec(
                        0.1f,
                        80f,
                        40f,
                        EXPLOSION_DAMAGE,
                        EXPLOSION_DAMAGE,
                        CollisionClass.PROJECTILE_NO_FF,
                        CollisionClass.PROJECTILE_FIGHTER,
                        3f, 3f, 1f, 0, Color.BLACK, Color.BLACK);
                explosionSpec.setUseDetailedExplosion(false);
                params = RiftCascadeMineExplosion.createStandardRiftParams(new Color(255, 50, 0), 25f);
                params.noiseMag = 3f;
                params.withNegativeParticles = false;
                params.fadeOut = 0.5f;
                params.fadeIn = 0.5f;
                params.hitGlowSizeMult = 0.4f;
                params.invertForDarkening = new Color(100, 25, 0);
                params.numRiftsToSpawn = 1;
                params.blackColor = Color.BLACK;
                params.underglow = new Color(100, 25, 0);
                params.noiseMult = 2f;
                params.noisePeriod = 0.2f;
                params.additiveBlend = true;
                VariantLookup.VariantInfo info = VariantLookup.getVariantInfo(ship.getVariant());
                if (ship.getFleetCommander() != null) {
                    masteryStrength = MasteryUtils.getModifiedMasteryEffectStrength(
                            ship.getFleetCommander(),
                            info.root == null ? ship.getHullSpec() : info.root.getHullSpec(),
                            1f);
                } else {
                    masteryStrength = 1f;
                }
            }

            @Override
            public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
                if (param instanceof CombatEntityAPI entity && entity.getCustomData() != null && (Boolean) entity.getCustomData().getOrDefault(EXPLOSION_ID_KEY, Boolean.FALSE)) {
                    if (!(target instanceof ShipAPI targetShip)) return null;
                    var listeners = targetShip.getListeners(DebuffScript.class);
                    if (listeners.isEmpty()) {
                        var script = new DebuffScript(targetShip, id);
                        script.addStack(DAMAGE_TAKEN_PER_STACK, MAX_DAMAGE_TAKEN, DURATION_SECONDS);
                        targetShip.addListener(script);
                    } else {
                        listeners.forEach(x -> x.addStack(DAMAGE_TAKEN_PER_STACK, MAX_DAMAGE_TAKEN, DURATION_SECONDS));
                    }
                    return null;
                }

                if (cooldown <= 0f) {
                    cooldown = MIN_DELAY_BETWEEN_SHOTS_SECONDS[Utils.hullSizeToInt(ship.getHullSize())] / masteryStrength;
                    var explosion = Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, ship, point, false);
                    explosion.setCustomData(EXPLOSION_ID_KEY, true);
                    RiftCascadeMineExplosion.spawnStandardRift(explosion, params);
                    Global.getSoundPlayer().playSound("riftcascade_rift", 0.6f, 1f, point, new Vector2f());
                }
                return null;
            }

            @Override
            public void advanceIfAlive(float amount) {
                cooldown -= amount;
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
            info.addPara(Strings.Skills.warpedKnowledgeEliteEffect1, 0f, hc, hc,
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[0]),
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[1]),
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[2]),
                    Utils.asFloatOneDecimal(MIN_DELAY_BETWEEN_SHOTS_SECONDS[3]),
                    Utils.asInt(EXPLOSION_DAMAGE),
                    Utils.asPercent(DAMAGE_TAKEN_PER_STACK),
                    Utils.asFloatOneDecimal(DURATION_SECONDS));
            info.addPara(Strings.Skills.warpedKnowledgeEliteEffect2, 0f, tc, hc, Utils.asPercent(MAX_DAMAGE_TAKEN));
            info.addPara(Strings.Skills.warpedKnowledgeEliteEffect3, tc, 0f);
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
