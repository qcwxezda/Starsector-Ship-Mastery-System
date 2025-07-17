package shipmastery.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.combat.listeners.AdvanceIfAliveListener;
import shipmastery.fx.OverlayEmitter;
import shipmastery.util.EngineUtils;
import shipmastery.util.MathUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class HiddenEffectScript extends AdvanceIfAliveListener implements PseudocoreHiddenSkillScript {

    public interface Provider {
        default float getCooldownSeconds(ShipAPI ship) {
            return 60f;
        }
        default float getDurationSeconds(ShipAPI ship) {
            return 6f;
        }
        default float getEffectStrength(ShipAPI ship) {
            return 0.75f;
        }
    }

    protected Color color;
    protected final String id;
    private final Provider plugin;
    private final Map<ShipAPI, OverlayEmitter> emitterMap = new HashMap<>();
    private float cooldownRemaining = 0f;
    private boolean active = false;
    private float activeTime = 0f;
    private final FaderUtil effectFader = new FaderUtil(0f, 0.5f);
    private final IntervalUtil repopulateWingsAndModulesInterval = new IntervalUtil(0.5f, 0.5f);
    private List<ShipAPI> allModules = new ArrayList<>();
    private List<ShipAPI> allWings = new ArrayList<>();
    public static final String IS_ACTIVE_KEY = "sms_HiddenEffectActive";

    private OverlayEmitter getEmitterForShip(ShipAPI ship) {
        var existing = emitterMap.get(ship);
        if (existing != null) {
            return existing;
        }
        var emitter = new OverlayEmitter(ship, ship.getSpriteAPI(), 0.8f);
        emitter.randomOffset = Math.min(ship.getSpriteAPI().getHeight(), ship.getSpriteAPI().getWidth()) / 7f;
        emitter.randomAngle = 20f;
        emitter.color = color;
        emitter.alphaMult = 0.2f;
        emitter.fadeInFrac = 0.2f;
        emitter.fadeOutFrac = 0.2f;
        emitter.enableDynamicAnchoring();
        emitterMap.put(ship, emitter);
        return emitter;
    }

    public HiddenEffectScript(ShipAPI ship, String id, Color overlayColor, Provider plugin) {
        super(ship);
        this.id = id;
        this.color = overlayColor;
        this.plugin = plugin;
        ship.setExplosionFlashColorOverride(color);
        resetCooldownTime();
        repopulateWingsAndModules();
    }

    @Override
    public final void activate() {
        effectFader.fadeIn();
        active = true;
        activeTime = 0f;
        resetCooldownTime();
        allModules.forEach(module -> module.setCustomData(IS_ACTIVE_KEY, true));
    }

    public final void deactivate() {
        active = false;
        effectFader.fadeOut();
        allModules.forEach(module -> module.setCustomData(IS_ACTIVE_KEY, false));
    }

    protected final void resetCooldownTime() {
        float cooldown = plugin.getCooldownSeconds(ship);
        cooldownRemaining = MathUtils.randBetween(0.8f*cooldown, 1.25f*cooldown);
    }

    protected final void applyEffects(float effectLevel, boolean shouldBurst) {
        allWings.forEach(wing -> {
            if (!Global.getCombatEngine().isShipAlive(wing)) return;
            if (effectLevel > 0f) {
                applyEffectsToShip(wing, effectLevel);
                wing.setCircularJitter(true);
                wing.setJitterShields(true);
                wing.setJitter(wing, color, effectFader.getBrightness(), 12, 10f);
            } else {
                unapplyEffectsToShip(wing);
            }
        });

        allModules.forEach(module -> {
            if (!Global.getCombatEngine().isShipAlive(module)) return;
            if (effectLevel > 0f) {
                applyEffectsToShip(module, effectLevel);
                if (shouldBurst && module.getSpriteAPI() != null) {
                    getEmitterForShip(module).burst(1);
                }
            } else {
                unapplyEffectsToShip(module);
            }
        });
    }

    protected final void repopulateWingsAndModules() {
        allModules = EngineUtils.getAllModules(ship);
        var wings = ship.getAllWings();
        allModules.forEach(module -> wings.addAll(module.getAllWings()));
        allWings = wings.stream()
                .map(FighterWingAPI::getWingMembers)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public final void advanceIfAlive(float amount) {
        float effectLevel = effectFader.getBrightness() * plugin.getEffectStrength(ship);
        applyEffects(effectLevel, Misc.random.nextFloat() < amount*9f);
        effectFader.advance(amount);

        repopulateWingsAndModulesInterval.advance(amount);
        if (repopulateWingsAndModulesInterval.intervalElapsed()) {
            repopulateWingsAndModules();
        }

        if (!active) {
            cooldownRemaining -= amount;
            if (cooldownRemaining <= 0f) {
                activate();
            }
        } else {
            activeTime += amount;
            if (activeTime >= plugin.getDurationSeconds(ship)) {
                deactivate();
            }
        }
    }

    protected abstract void applyEffectsToShip(ShipAPI ship, float effectLevel);
    protected abstract void unapplyEffectsToShip(ShipAPI ship);
}
