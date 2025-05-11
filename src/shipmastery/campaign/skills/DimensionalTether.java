package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.impl.combat.RealityDisruptorChargeGlow;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.JitterEmitter;
import shipmastery.fx.RingBurstEmitter;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimensionalTether {

    public static final float[] HULL_REPAIR_PER_SECOND = {0.04f, 0.02f, 0.01f, 0.005f};
    public static final float ARMOR_REPAIR_MULT = 0.5f;
    public static final float RETREAT_DELAY = 1.5f;
    public static final float ADD_TO_RESERVE_DELAY = 10f;
    // No CombatEngine.getPlugins or similar, so we need to track the existing repair scripts ourselves
    public static final String EXISTING_REPAIR_SCRIPTS_KEY = "$sms_DimensionalTetherScripts";
    public static final String HAS_ELITE_EFFECT_KEY = "$sms_EliteDimensionalTether";
    public static final float AI_CORE_COMMANDER_REPAIR_MULT = 3f;
    public static final float[] EMP_RANGE = {800f, 1000f, 1250f, 1500f};

    private static class RepairScript extends BaseEveryFrameCombatPlugin {
        private final IntervalUtil updateInterval = new IntervalUtil(0.5f, 0.5f);
        private final ShipAPI ship;
        private final boolean commanderisAICore;

        private RepairScript(ShipAPI ship) {
            this.ship = ship;
            commanderisAICore = ship.getFleetMember() != null && ship.getFleetMember().getFleetCommanderForStats() != null && ship.getFleetMember().getFleetCommanderForStats().isAICore();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) return;
            updateInterval.advance(amount);
            if (updateInterval.intervalElapsed()) {
                FleetMemberAPI fm = ship.getFleetMember();
                if (fm == null) return;
                CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(ship.getOriginalOwner());
                // Don't repair if you've deployed no ships to avoid stalling
                if (fleetManager.getDeployedCopy().isEmpty()) return;
                float dur = updateInterval.getIntervalDuration();
                if (fleetManager.getReservesCopy().contains(fm)) {
                    float repairAmount = dur * HULL_REPAIR_PER_SECOND[Utils.hullSizeToInt(ship.getHullSize())];
                    if (commanderisAICore) {
                        repairAmount *= AI_CORE_COMMANDER_REPAIR_MULT;
                    }
                    fm.getStatus().repairHullFraction(repairAmount);
                    fm.getStatus().repairArmorAllCells(repairAmount * ARMOR_REPAIR_MULT);
                }
            }
        }
    }

    private static class RetreatScript implements HullDamageAboutToBeTakenListener, AdvanceableListener {
        private final ShipAPI ship;
        private boolean isRetreating = false;
        private float retreatTime = 0f;

        private RetreatScript(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (isRetreating) {
                ship.setExtraAlphaMult2((RETREAT_DELAY - retreatTime) / RETREAT_DELAY);
                retreatTime += amount;
            }
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            FleetMemberAPI fm = ship.getFleetMember();
            if (fm == null) return false;
            if (damageAmount >= ship.getHitpoints() && !isRetreating && fm.getRepairTracker().getCR() >= fm.getDeployCost()) {
                isRetreating = true;
                JitterEmitter emitter = new JitterEmitter(ship, ship.getSpriteAPI(), new Color(150, 250, 200), 0f, 70f, 0.25f, false, 0.3f, 100);
                emitter.setBaseIntensity(0.8f);
                emitter.enableDynamicAnchoring();
                Particles.stream(emitter, 1, 80f, 1.25f);
                CombatDeferredActionPlugin.performLater(() -> {
                    // EMP blast
                    if (ship.getCustomData().containsKey(HAS_ELITE_EFFECT_KEY)) {
                        float radius = EMP_RANGE[Utils.hullSizeToInt(ship.getHullSize())];
                        var loc = new Vector2f(ship.getLocation());
                        RingBurstEmitter ringEmitter = new RingBurstEmitter(loc, 50f, radius, 10f);
                        ringEmitter.burst(360);
                        CombatDeferredActionPlugin.performLater(() -> ringEmitter.burst(360), 0.1f);
                        CombatDeferredActionPlugin.performLater(() -> ringEmitter.burst(360), 0.2f);
                        CombatDeferredActionPlugin.performLater(() -> {
                            for (var itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(loc, 2f * radius, 2f * radius); itr.hasNext(); ) {
                                var target = itr.next();
                                if (!(target instanceof CombatEntityAPI entity)) continue;
                                var dist = MathUtils.dist(entity.getLocation(), loc);
                                if (dist > radius + entity.getCollisionRadius()) continue;
                                if (entity instanceof MissileAPI missile) {
                                    missile.flameOut();
                                } else if (entity instanceof ShipAPI targetShip) {
                                    List<Vector2f> targetLocs = new ArrayList<>();
                                    for (WeaponAPI weapon : targetShip.getUsableWeapons()) {
                                        targetLocs.add(weapon.getLocation());
                                    }
                                    for (ShipEngineControllerAPI.ShipEngineAPI engine : targetShip.getEngineController().getShipEngines()) {
                                        targetLocs.add(engine.getLocation());
                                    }
                                    for (var targetLoc : targetLocs) {
                                        float emp = MathUtils.randBetween(0f, 150000f); // Yes, really, 50000 isn't enough to even disable 1 invictus weapon
                                        if (Misc.random.nextFloat() < 0.75f) {
                                            Global.getCombatEngine().applyDamage(null, targetShip, targetLoc, 0f, DamageType.ENERGY, emp, true, true, ship, true);
                                        }
                                    }
                                    List<RealityDisruptorChargeGlow.RDRepairRateDebuff> listeners = targetShip.getListeners(RealityDisruptorChargeGlow.RDRepairRateDebuff.class);
                                    if (listeners.isEmpty()) {
                                        targetShip.addListener(new RealityDisruptorChargeGlow.RDRepairRateDebuff(targetShip, 10f));
                                    } else {
                                        listeners.get(0).resetDur(10f);
                                    }
                                }
                            }
                        }, 0.3f);
                        for (int i = 0; i < 180; i++) {
                            Vector2f from = MathUtils.randomPointInCircle(loc, ship.getShieldRadiusEvenIfNoShield() / 3f);
                            Vector2f pt = MathUtils.randomPointInCircle(loc, radius);
                            EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                            params.segmentLengthMult = 5f;
                            params.zigZagReductionFactor = 0.3f;
                            params.minFadeOutMult = 100f;
                            params.flickerRateMult = 0.25f;
                            params.nonBrightSpotMinBrightness = 0f;
                            params.movementDurMin = 0.5f;
                            params.movementDurMax = 0.9f;
                            float arcSize = 50f + MathUtils.randBetween(0f, 50f);
                            var arc = Global.getCombatEngine().spawnEmpArcVisual(from, null, pt, null, arcSize, new Color(50, 150, 100, 100), new Color(150, 250, 200), params);
                            arc.setCoreWidthOverride(arcSize / 2f);
                            arc.setRenderGlowAtStart(false);
                            arc.setSingleFlickerMode(true);
                        }
                    }
                    // Retreat
                    Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    Global.getSoundPlayer().playSound("sms_dimensional_tether_retreat", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    ship.setOwner(ship.getOriginalOwner()); // No mind control nonsense
                    ship.setRetreating(true, true);
                    ship.getLocation().set(0f, -1000000f);
                    // Add to reserves later
                    CombatDeferredActionPlugin.performLater(() -> {
                        if (fm.getCaptain().isPlayer()) {
                            if (fm.isFlagship()) {
                                fm.setFlagship(false);
                            }
                            fm.setCaptain(null);
                        }
                        float crLoss = fm.getDeployCost();
                        fm.getRepairTracker().applyCREvent(-crLoss, Strings.Skills.dimensionalTetherRetreatText);
                        Global.getCombatEngine().getFleetManager(ship.getOriginalOwner()).addToReserves(fm);
                    }, ADD_TO_RESERVE_DELAY);
                }, RETREAT_DELAY);
                return true;
            }
            return isRetreating;
        }
    }

    public static class Standard extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (ship.getFleetMemberId() != null) {
                //noinspection unchecked
                Map<String, RepairScript> existingScripts = (Map<String, RepairScript>) Global.getCombatEngine().getCustomData().computeIfAbsent(EXISTING_REPAIR_SCRIPTS_KEY, k -> new HashMap<>());
                if (!existingScripts.containsKey(ship.getFleetMemberId())) {
                    var script = new RepairScript(ship);
                    Global.getCombatEngine().addPlugin(script);
                    existingScripts.put(ship.getFleetMemberId(), script);
                }
            }
            ship.addListener(new RetreatScript(ship));
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(RetreatScript.class);
            if (ship.getFleetMemberId() != null) {
                //noinspection unchecked
                Map<String, RepairScript> existingScripts = (Map<String, RepairScript>) Global.getCombatEngine().getCustomData().computeIfAbsent(EXISTING_REPAIR_SCRIPTS_KEY, k -> new HashMap<>());
                var script = existingScripts.get(ship.getFleetMemberId());
                if (script != null) {
                    Global.getCombatEngine().removePlugin(script);
                    existingScripts.remove(ship.getFleetMemberId());
                }
            }
        }

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            info.addPara(Strings.Skills.dimensionalTetherEffect, 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), Utils.asPercent(HULL_REPAIR_PER_SECOND[0]), Utils.asPercent(HULL_REPAIR_PER_SECOND[1]), Utils.asPercent(HULL_REPAIR_PER_SECOND[2]), Utils.asPercent(HULL_REPAIR_PER_SECOND[3]), Utils.asPercent(ARMOR_REPAIR_MULT));
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {

        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            info.addPara(Strings.Skills.dimensionalTetherEliteEffect, 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), skill.getName(), Utils.asInt(EMP_RANGE[0]), Utils.asInt(EMP_RANGE[1]), Utils.asInt(EMP_RANGE[2]), Utils.asInt(EMP_RANGE[3]));
        }

        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.setCustomData(HAS_ELITE_EFFECT_KEY, true);
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeCustomData(HAS_ELITE_EFFECT_KEY);
        }
    }
}
