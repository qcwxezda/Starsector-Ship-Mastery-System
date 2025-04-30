package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;
import particleengine.Particles;
import shipmastery.config.Settings;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.JitterEmitter;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExtradimensionalRearrangement5 extends BaseHullMod {

    public static final float[] HULL_REPAIR_PER_SECOND = {0.04f, 0.02f, 0.01f, 0.005f};
    public static final float ARMOR_REPAIR_MULT = 0.5f;
    public static final float RETREAT_DELAY = 1.5f;
    public static final float ADD_TO_RESERVE_DELAY = 3f;
    public static final float UPKEEP_INCREASE = 2f;
    // No CombatEngine.getPlugins or similar, so we need to track the existing repair scripts ourselves
    public static final String EXISTING_REPAIR_SCRIPTS_KEY = "$sms_ExtradimensionalRearrangement5Scripts";

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
                    float repairAmount = dur*HULL_REPAIR_PER_SECOND[Utils.hullSizeToInt(ship.getHullSize())];
                    fm.getStatus().repairHullFraction(repairAmount);
                    fm.getStatus().repairArmorAllCells(repairAmount*ARMOR_REPAIR_MULT);
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
                ship.setExtraAlphaMult2((RETREAT_DELAY - retreatTime)/RETREAT_DELAY);
                retreatTime += amount;
            }
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
            if (damageAmount >= ship.getHitpoints() && !isRetreating) {
                isRetreating = true;
                JitterEmitter emitter = new JitterEmitter(
                        ship,
                        ship.getSpriteAPI(),
                        ship.getSpriteAPI().getAverageColor(),
                        0f,
                        70f,
                        0.25f,
                        false,
                        0.3f,
                        100);
                emitter.setBaseIntensity(0.8f);
                emitter.enableDynamicAnchoring();
                Particles.stream(emitter, 1, 80f, 1.25f);
                CombatDeferredActionPlugin.performLater(() -> {
                    Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
                    ship.setRetreating(true, true);
                    ship.getLocation().set(0f, -1000000f);
                    CombatDeferredActionPlugin.performLater(() -> {
                        FleetMemberAPI fm = ship.getFleetMember();
                        if (fm == null) return;
                        if (fm.getCaptain().isPlayer()) {
                            if (fm.isFlagship()) {
                                fm.setFlagship(false);
                            }
                            fm.setCaptain(null);
                        }
                        float crLoss = fm.getDeployCost();
                        fm.getRepairTracker().applyCREvent(-crLoss, Strings.Hullmods.rearrangement5RetreatText);
                        if (fm.getRepairTracker().getCR() > 0f) {
                            Global.getCombatEngine().getFleetManager(ship.getOriginalOwner()).addToReserves(fm);
                        }
                    }, ADD_TO_RESERVE_DELAY);
                }, RETREAT_DELAY);
                return true;
            }
            return isRetreating;
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getSuppliesPerMonth().modifyPercent(id, 100f*UPKEEP_INCREASE);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getFleetMemberId() != null) {
            //noinspection unchecked
            Set<String> existingScripts = (Set<String>) Global.getCombatEngine().getCustomData().computeIfAbsent(EXISTING_REPAIR_SCRIPTS_KEY, k -> new HashSet<>());
            if (!existingScripts.contains(ship.getFleetMemberId())) {
                Global.getCombatEngine().addPlugin(new RepairScript(ship));
                existingScripts.add(ship.getFleetMemberId());
            }
        }
        ship.addListener(new RetreatScript(ship));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara(
                Strings.Hullmods.rearrangement5Effect,
                8f,
                Settings.POSITIVE_HIGHLIGHT_COLOR,
                Utils.asPercent(HULL_REPAIR_PER_SECOND[0]),
                Utils.asPercent(HULL_REPAIR_PER_SECOND[1]),
                Utils.asPercent(HULL_REPAIR_PER_SECOND[2]),
                Utils.asPercent(HULL_REPAIR_PER_SECOND[3]),
                Utils.asPercent(ARMOR_REPAIR_MULT),
                Utils.asPercent(UPKEEP_INCREASE));
    }

    @Override
    public Color getBorderColor() {
        return Settings.MASTERY_COLOR;
    }

    @Override
    public Color getNameColor() {
        return Settings.MASTERY_COLOR;
    }
}
