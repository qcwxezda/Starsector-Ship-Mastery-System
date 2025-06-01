package shipmastery.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.fx.OverlayEmitter;
import shipmastery.util.MathUtils;
import shipmastery.util.Utils;
import shipmastery.util.VariantLookup;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CuratorNPCHullmod extends BaseHullMod {

    public static final String CUSTOM_DATA_KEY = "sms_FightersList";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats.getFleetMember() == null
                || stats.getFleetMember().getFleetData() == null
                || stats.getFleetMember().getFleetData().getCommander() == null
                || stats.getFleetMember().getFleetData().getCommander().isPlayer()) return;
        stats.getMinCrewMod().modifyMult(id, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        var info = VariantLookup.getVariantInfo(ship.getVariant());
        boolean isRoot = info == null || info.root == info.variant;
        if (isRoot) {
            ship.addListener(new CuratorNPCHullmodScript(ship, id, null));
        } else {
            // parent station is null initially, need to wait for it to be populated...
            CombatDeferredActionPlugin.performLater(() -> {
                CuratorNPCHullmodScript rootListener = null;
                var root = ship.getParentStation();
                if (root != null) {
                    var listeners = root.getListeners(CuratorNPCHullmodScript.class);
                    if (listeners != null && !listeners.isEmpty()) {
                        rootListener = listeners.iterator().next();
                    }
                }
                ship.addListener(new CuratorNPCHullmodScript(ship, id, rootListener));
            }, 0f);
        }
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        //noinspection unchecked
        List<ShipAPI> fighterList = ship.getCustomData() == null ? null : (List<ShipAPI>) ship.getCustomData().get(CUSTOM_DATA_KEY);
        if (fighterList == null) {
            fighterList = new LinkedList<>();
            fighterList.add(fighter);
            ship.setCustomData(CUSTOM_DATA_KEY, fighterList);
        }
        fighterList.add(fighter);
    }

    protected float getBaseCooldownGamma() {
        return 60f;
    }

    protected float getBaseCooldownBeta() {
        return 45f;
    }

    protected float getBaseCooldownAlpha() {
        return 30f;
    }

    protected float getBaseCooldownOmega() {
        return 24f;
    }

    protected float getDurationSeconds() {
        return 6f;
    }

    protected float getDamageReductionAmount() {
        return 0.25f;
    }

    protected boolean isNPCVersion() {
        return true;
    }

    public class CuratorNPCHullmodScript implements AdvanceableListener, ShipDestroyedListener {
        private final ShipAPI ship;
        private final CuratorNPCHullmodScript rootShipListener;
        private final String id;
        private final OverlayEmitter emitter;
        private boolean active = false;
        private float activeTime = 0f;
        private final FaderUtil effectFader = new FaderUtil(0f, 0.5f);
        private final boolean isOmegaAndNPC;
        private float cooldownMult = 1f;
        private float cooldownTime;
        private float baseCooldownTime;
        public static final Color color = new Color(100, 200, 150);

        private OverlayEmitter makeEmitter(ShipAPI ship) {
            var emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 1f);
            emitter.randomOffset = Math.min(ship.getSpriteAPI().getHeight(), ship.getSpriteAPI().getWidth()) / 7f;
            emitter.randomAngle = 20f;
            emitter.color = color;
            emitter.alphaMult = 0.3f;
            emitter.fadeInFrac = 0.2f;
            emitter.fadeOutFrac = 0.2f;
            emitter.enableDynamicAnchoring();
            return emitter;
        }

        public CuratorNPCHullmodScript(ShipAPI ship, String id, @Nullable CuratorNPCHullmodScript rootShipListener) {
            this.ship = ship;
            this.rootShipListener = rootShipListener;

            this.id = id;
            if (ship.getSpriteAPI() == null) {
                emitter = null;
            } else {
                emitter = makeEmitter(ship);
            }

            String coreId;
            if (ship.getCaptain() != null && (coreId = ship.getCaptain().getAICoreId()) != null) {
                isOmegaAndNPC = isNPCVersion() && "sms_amorphous_core".equals(coreId);
                baseCooldownTime = switch (coreId) {
                    case "sms_gamma_k_core" -> getBaseCooldownGamma();
                    case "sms_beta_k_core" -> getBaseCooldownBeta();
                    case "sms_alpha_k_core" -> getBaseCooldownAlpha();
                    case "sms_amorphous_core" -> getBaseCooldownOmega();
                    default -> 999999999f;
                };
            } else {
                baseCooldownTime = 999999999f;
                isOmegaAndNPC = false;
            }
            boolean enabled = baseCooldownTime < 100f;
            if (enabled) {
                ship.setExplosionFlashColorOverride(new Color(150, 250, 200));
            } else {
                ship.removeListener(this);
            }

            if (isOmegaAndNPC) {
                // Need to wait until ship's CR is actually set
                CombatDeferredActionPlugin.performLater(() -> {
                    baseCooldownTime *= ship.getCurrentCR();
                    resetCooldownTime();
                }, 0f);
                var stats = ship.getMutableStats();
                stats.getCriticalMalfunctionChance().modifyMult(id, 0f);
                stats.getWeaponMalfunctionChance().modifyMult(id, 0f);
                stats.getEngineMalfunctionChance().modifyMult(id, 0f);
                stats.getShieldMalfunctionChance().modifyMult(id, 0f);
            }

            resetCooldownTime();
        }

        private void resetCooldownTime() {
            cooldownTime = MathUtils.randBetween(0.8f*baseCooldownTime*cooldownMult, 1.25f*baseCooldownTime*cooldownMult);
        }

        private void applyEffects(ShipAPI ship, float effectLevel) {
            if (effectLevel > 0f) {
                ship.getMutableStats().getTimeMult().modifyMult(id, 1f + effectLevel);
                ship.getMutableStats().getEmpDamageTakenMult().modifyMult(id, 1f - effectLevel*getDamageReductionAmount());
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, 1f - effectLevel*getDamageReductionAmount());
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, 1f - effectLevel*getDamageReductionAmount());
                ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, 1f - effectLevel*getDamageReductionAmount());
            } else {
                ship.getMutableStats().getTimeMult().unmodify(id);
                ship.getMutableStats().getEmpDamageTakenMult().unmodify(id);
                ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
            }
        }

        public void activate() {
            if (rootShipListener != null) return;
            effectFader.fadeIn();
            active = true;
            activeTime = 0f;
            resetCooldownTime();
        }

        @Override
        public void advance(float amount) {
            var engine = Global.getCombatEngine();
            if (!engine.isShipAlive(ship)) {
                ship.removeListener(this);
                return;
            }

            if (rootShipListener == null) {
                effectFader.advance(amount);
            } else {
                effectFader.setBrightness(rootShipListener.effectFader.getBrightness());
            }

            //noinspection unchecked
            List<ShipAPI> fighterList = ship.getCustomData() == null ? null : (List<ShipAPI>) ship.getCustomData().get(CUSTOM_DATA_KEY);
            if (fighterList == null) fighterList = new LinkedList<>();

            float effectLevel = effectFader.getBrightness();
            applyEffects(ship, effectLevel);

            for (Iterator<ShipAPI> iterator = fighterList.iterator(); iterator.hasNext(); ) {
                ShipAPI fighter = iterator.next();
                if (!engine.isShipAlive(fighter)) {
                    iterator.remove();
                    continue;
                }
                applyEffects(fighter, effectLevel);
            }

            if (rootShipListener != null) {
                active = rootShipListener.active;
            }

            if (!active && rootShipListener == null) {
                cooldownTime -= amount;
                if (cooldownTime <= 0f) {
                    activate();
                }
            } else if (active) {
                if (Misc.random.nextFloat() < 0.1f && emitter != null) {
                    emitter.burst(1);
                }
                if (rootShipListener == null) {
                    activeTime += amount;
                    if (activeTime >= getDurationSeconds()) {
                        active = false;
                        effectFader.fadeOut();
                    }
                }
            }
            if (effectFader.getBrightness() > 0f) {
                for (ShipAPI fighter : fighterList) {
                    fighter.setCircularJitter(true);
                    fighter.setJitterShields(true);
                    fighter.setJitter(fighter, color, effectFader.getBrightness(), 12, 10f);
                }
            }
        }

        @Override
        public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
            if (!isOmegaAndNPC) return;
            if (target.isFighter()) return;
            if (target.getOriginalOwner() == ship.getOwner()) {
                float reduction = Utils.hullSizeToInt(target.getHullSize()) * 0.03f;
                cooldownMult = Math.max(0f, cooldownMult - reduction);
                float dist = MathUtils.dist(target.getLocation(), ship.getLocation());
                if (dist > 5000f) return;

                for (int i = 0; i < 5; i++) {
                    EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
                    params.segmentLengthMult = 4f;
                    params.zigZagReductionFactor = 0.05f;
                    params.minFadeOutMult = 1f;
                    params.flickerRateMult = MathUtils.randBetween(150f, 250f)/dist;
                    params.nonBrightSpotMinBrightness = 0f;

                    float arcSize = 50f + MathUtils.randBetween(0f, 200f);
                    var arc = Global.getCombatEngine().spawnEmpArcVisual(
                            target.getLocation(),
                            target,
                            ship.getLocation(),
                            ship, arcSize,
                            new Color(50, 150, 100, 100),
                            new Color(150, 250, 200),
                            params);
                    arc.setCoreWidthOverride(arcSize / 2f);
                    arc.setRenderGlowAtStart(false);
                    arc.setFadedOutAtStart(true);
                    arc.setSingleFlickerMode(true);
                }
            }
        }
    }
}
