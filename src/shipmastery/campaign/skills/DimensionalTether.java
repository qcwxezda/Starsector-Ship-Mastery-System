package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
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
import com.fs.starfarer.api.impl.hullmods.PhaseAnchor;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetMember;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.combat.entities.Ship;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.JitterEmitter;
import shipmastery.fx.RingBurstEmitter;
import shipmastery.util.EngineUtils;
import shipmastery.util.MathUtils;
import shipmastery.util.Strings;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DimensionalTether {

    public static final float[] HULL_REPAIR_PER_SECOND = {0.04f, 0.03f, 0.02f, 0.01f};
    public static final float ARMOR_REPAIR_MULT = 0.5f;
    public static final float RETREAT_DELAY = 1.5f;
    public static final float ADD_TO_RESERVE_DELAY_PLAYER = 3f;
    // Note: repair over time only applies to player ships, for NPC ships the delay to add to reserve is higher,
    // but they always enter with full health
    public static final float ADD_TO_RESERVE_DELAY_NPC = 25f;
    // No CombatEngine.getPlugins or similar, so we need to track the existing repair scripts ourselves
    public static final String EXISTING_REPAIR_SCRIPTS_KEY = "$sms_DimensionalTetherScripts";
    public static final float MIN_CR_COST = 0.1f;
    public static final float FLUX_REDUCTION_AMOUNT = 0.1f;
    public static final float[] EMP_RANGE = {800f, 1200f, 1600f, 2000f};
    public static final String REMOVED_PHASE_ANCHOR_KEY = "sms_RemovedPhaseAnchor";
    public static final String IS_RETREATING_KEY = "sms_DimensionalTetherIsRetreating";

    private static class RepairScript extends BaseEveryFrameCombatPlugin {
        private final IntervalUtil updateInterval = new IntervalUtil(0.5f, 0.5f);
        private final ShipAPI ship;

        private RepairScript(ShipAPI ship) {
            this.ship = ship;
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
        private CombatFleetManagerAPI fleetManager;

        private RetreatScript(ShipAPI ship) {
            this.ship = ship;
            CombatDeferredActionPlugin.performLater(() -> {
                fleetManager = Global.getCombatEngine().getFleetManager(ship.getOwner());
                // This is better than phase anchor, so should supersede it...
                if (ship.hasListenerOfClass(PhaseAnchor.PhaseAnchorScript.class)) {
                    ship.removeListenerOfClass(PhaseAnchor.PhaseAnchorScript.class);
                    ship.setCustomData(REMOVED_PHASE_ANCHOR_KEY, true);
                }
            }, 0f);
        }

        @Override
        public void advance(float amount) {
            if (isRetreating) {
                for (ShipAPI module : EngineUtils.getAllModules(ship)) {
                    module.setExtraAlphaMult2(Math.max(0f, (RETREAT_DELAY - retreatTime) / RETREAT_DELAY));
                }
                retreatTime += amount;
            }
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            FleetMemberAPI fm = ship.getFleetMember();
            if (fm == null) return false;
            if (fleetManager == null) return false;
            float crCost = Math.max(fm.getDeployCost(), MIN_CR_COST);
            if (damageAmount >= ship.getHitpoints() && !isRetreating && ship.getCurrentCR() >= crCost) {
                ship.setCustomData(IS_RETREATING_KEY, true);
                isRetreating = true;
                for (ShipAPI module : EngineUtils.getAllModules(ship)) {
                    JitterEmitter emitter = new JitterEmitter(module, module.getSpriteAPI(), new Color(150, 250, 200), 0f, module.getShieldRadiusEvenIfNoShield() / 2f, 0.25f, false, 0.3f, 100);
                    emitter.setBaseIntensity(0.8f);
                    emitter.enableDynamicAnchoring();
                    Particles.stream(emitter, 1, 80f, 1.25f);
                }
                CombatDeferredActionPlugin.performLater(() -> {
                    // EMP blast
                    float radius = EMP_RANGE[Utils.hullSizeToInt(ship.getHullSize())];
                    var loc = new Vector2f(ship.getLocation());
                    RingBurstEmitter ringEmitter = new RingBurstEmitter(loc, 50f, radius, 10f);
                    ringEmitter.burst(120);
                    CombatDeferredActionPlugin.performLater(() -> ringEmitter.burst(120), 0.1f);
                    CombatDeferredActionPlugin.performLater(() -> ringEmitter.burst(120), 0.2f);
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
                                    float emp = MathUtils.randBetween(0f, 150000f);
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
                                targetShip.getFluxTracker().forceOverload(15f * (radius + entity.getCollisionRadius() - dist) / radius);
                            }
                        }
                    }, 0.3f);
                    for (int i = 0; i < 360; i++) {
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
                    // Retreat
                    Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    Global.getSoundPlayer().playSound("sms_dimensional_tether_retreat", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    ship.setRetreating(true, true);
                    ((CombatFleetManager) fleetManager).retreat((Ship) ship);
                    fm.getRepairTracker().setCR(ship.getCurrentCR());
                    fm.getRepairTracker().applyCREvent(-crCost, Strings.Skills.dimensionalTetherRetreatText);
                    if (fleetManager.getOwner() == 1) {
                        fm.getRepairTracker().performRepairsFraction(1f);
                        ship.setLowestHullLevelReached(1f);
                    }
                    // Add to reserves later
                    CombatDeferredActionPlugin.performLater(() -> {
                        // Don't let NPC fleets keep deploying after combat is over
                        if (fleetManager.getOwner() == 1) {
                            if (Global.getCombatEngine().isCombatOver()) {
                                return;
                            }
                        }

                        if (fm.getCaptain().isPlayer()) {
                            if (fm.isFlagship()) {
                                fm.setFlagship(false);
                            }
                            fm.setCaptain(null);
                        }
                        fleetManager.addToReserves(fm);
                        ((CombatFleetManager) fleetManager).getRetreated().remove((FleetMember) fm);
                    }, fleetManager.getOwner() == 0 ? ADD_TO_RESERVE_DELAY_PLAYER : ADD_TO_RESERVE_DELAY_NPC);
                }, RETREAT_DELAY);
                return true;
            }
            return isRetreating;
        }
    }

    public static class Standard extends BaseSkillEffectDescription implements AfterShipCreationSkillEffect {
        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            var info = VariantLookup.getVariantInfo(ship.getVariant());
            boolean isRoot = info == null || info.root == info.variant;
            if (ship.getFleetMemberId() != null && ship.getVariant() != null && isRoot) {
                //noinspection unchecked
                Map<String, RepairScript> existingScripts = (Map<String, RepairScript>) Global.getCombatEngine().getCustomData().computeIfAbsent(EXISTING_REPAIR_SCRIPTS_KEY, k -> new HashMap<>());
                if (!existingScripts.containsKey(ship.getFleetMemberId())) {
                    var script = new RepairScript(ship);
                    Global.getCombatEngine().addPlugin(script);
                    existingScripts.put(ship.getFleetMemberId(), script);
                }
                ship.addListener(new RetreatScript(ship));
            }
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(RetreatScript.class);
            // If removed phase anchor, add it back
            if (ship.getCustomData() != null && (boolean) ship.getCustomData().getOrDefault(REMOVED_PHASE_ANCHOR_KEY, false)) {
                ship.addListener(new PhaseAnchor.PhaseAnchorScript(ship));
                ship.removeCustomData(REMOVED_PHASE_ANCHOR_KEY);
            }
            // Only add repair script for player, NPCs start with full repairs
            var info = VariantLookup.getVariantInfo(ship.getVariant());
            boolean isRoot = info == null || info.root == info.variant;
            if (ship.getFleetMemberId() != null
                    && ship.getFleetCommander() != null
                    && ship.getFleetCommander().isPlayer()
                    && ship.getVariant() != null
                    && isRoot) {
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
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {}

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {}

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            info.addPara(Strings.Skills.dimensionalTetherEffect, 0f, Misc.getHighlightColor(), Misc.getHighlightColor(),
                    Utils.asInt(EMP_RANGE[0]),
                    Utils.asInt(EMP_RANGE[1]),
                    Utils.asInt(EMP_RANGE[2]),
                    Utils.asInt(EMP_RANGE[3]),
                    Utils.asPercent(MIN_CR_COST),
                    Utils.asPercent(HULL_REPAIR_PER_SECOND[0]),
                    Utils.asPercent(HULL_REPAIR_PER_SECOND[1]),
                    Utils.asPercent(HULL_REPAIR_PER_SECOND[2]),
                    Utils.asPercent(HULL_REPAIR_PER_SECOND[3]),
                    Utils.asPercent(ARMOR_REPAIR_MULT));
        }
    }

    public static class Elite extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION_AMOUNT);
            stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION_AMOUNT);
            stats.getMissileWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION_AMOUNT);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getBallisticWeaponFluxCostMod().unmodify(id);
            stats.getEnergyWeaponFluxCostMod().unmodify(id);
            stats.getMissileWeaponFluxCostMod().unmodify(id);
        }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            info.addPara(Strings.Skills.dimensionalTetherEliteEffect, 0f, Misc.getHighlightColor(), Misc.getHighlightColor(), Utils.asPercent(FLUX_REDUCTION_AMOUNT));
        }
    }
}
