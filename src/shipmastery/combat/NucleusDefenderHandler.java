package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.campaign.skills.PseudocoreHiddenSkillScript;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.fx.ParticleBurstEmitter;
import shipmastery.fx.StaticRingEmitter;
import shipmastery.util.MathUtils;
import shipmastery.util.Utils;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class NucleusDefenderHandler extends BaseEveryFrameCombatPlugin implements ShipDestroyedListener {

    public static final float RADIUS = 2250f;
    public static final float MIN_DURATION = 20f;
    public static final float MAX_DURATION = 30f;
    public static final int MAX_REGIONS = 10;
    private final float baseCooldown = 25f;
    private float cooldownMult = 1f;
    private float cooldown = baseCooldown;
    private final Map<Vector2f, LocationData> existingLocs = new TreeMap<>((a, b) -> {
        int cmp1 = Float.compare(a.x, b.x);
        if (cmp1 != 0) return cmp1;
        return Float.compare(a.y, b.y);
    });

    static final class LocationData {
        private final float totalTime;
        private float timeLeft;
        private float radius;
        private final ParticleBurstEmitter burstEmitter;
        private final StaticRingEmitter ringEmitter;
        private final IntervalUtil ringInterval = new IntervalUtil(0.75f, 0.75f);

        LocationData(Vector2f loc, float totalTime, float timeLeft, float radius) {
            this.totalTime = totalTime;
            this.timeLeft = timeLeft;
            this.radius = radius;
            burstEmitter = new ParticleBurstEmitter(loc);
            burstEmitter.radius = 50f;
            burstEmitter.color = new Color(0.5f, 1f, 0.75f);
            burstEmitter.alpha = 0.2f;
            burstEmitter.lengthMultiplierOverTime = 5f;
            burstEmitter.size = 15f;
            burstEmitter.sizeJitter = 2.5f;
            ringEmitter = new StaticRingEmitter(loc);
            ringEmitter.color = new Color(0.5f, 1f, 0.75f, 0.1f);
        }
    }

    public NucleusDefenderHandler() {
        Global.getCombatEngine().getListenerManager().addListener(this);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) return;
        cooldown -= amount;
        if (cooldown <= 0f && existingLocs.size() < MAX_REGIONS) {
            Vector2f loc = pickLocation();
            float time = MathUtils.randBetween(MIN_DURATION, MAX_DURATION);
            existingLocs.put(loc, new LocationData(loc, time, time, 0f));
            cooldown = baseCooldown * cooldownMult;
        }

        for (Iterator<Map.Entry<Vector2f, LocationData>> iterator = existingLocs.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            var loc = entry.getKey();
            var data = entry.getValue();
            if (data.timeLeft > data.totalTime / 2f) {
                data.radius = 0f;
                data.burstEmitter.burst(3);
            } else {
                float nearTime = Math.min(data.timeLeft, Math.abs(data.timeLeft - data.totalTime/2f));
                data.radius = Math.min(1f, nearTime / 3f) * RADIUS;
                if (data.timeLeft > 3f) {
                    data.ringEmitter.endRadius = RADIUS;
                    data.ringEmitter.life = 3f;
                    data.ringInterval.advance(amount);
                    if (data.ringInterval.intervalElapsed()) {
                        data.ringEmitter.burst(1);
                    }
                }
            }
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (MathUtils.dist(loc, ship.getLocation()) <= data.radius) {
                    var listeners = ship.getListeners(PseudocoreHiddenSkillScript.class);
                    if (listeners != null) {
                        for (var listener : listeners) {
                            listener.activate();
                        }
                    }
                }
            }
            data.timeLeft -= amount;
            if (data.timeLeft <= 0f) {
                iterator.remove();
            }
        }
    }

    private float computeScore(Vector2f loc) {
        float score = 0f;
        for (Vector2f existing : existingLocs.keySet()) {
            // Too close to existing one
            if (MathUtils.dist(loc, existing) <= RADIUS) {
                return score;
            }
        }
        for (var itr = Global.getCombatEngine().getShipGrid().getCheckIterator(loc, 2f*RADIUS, 2f*RADIUS); itr.hasNext(); ) {
            if (!(itr.next() instanceof ShipAPI ship)) {
                continue;
            }

            if (MathUtils.dist(ship.getLocation(), loc) > RADIUS * 3f/4f) continue;
            float thisScore = Utils.hullSizeToInt(ship.getHullSize()) + 1f;
            if (ship.getOwner() != -1) thisScore *= 0.5f;
            score += thisScore;
        }
        return score;
    }

    private Vector2f pickLocation() {
        var engine = Global.getCombatEngine();

        float bestScore = 0f;
        Vector2f bestLoc = new Vector2f();
        var objectives = engine.getObjectives();
        if (objectives != null) {
            for (var obj : objectives) {
                float score = computeScore(obj.getLocation()) + 3f;
                if (score > bestScore) {
                    bestScore = score;
                    bestLoc = obj.getLocation();
                }
            }
        }

        // Now try some random locations near enemy ships
        var ships = engine.getShips();
        for (var checker : ships) {
            Vector2f loc = MathUtils.randomPointInCircle(checker.getLocation(), RADIUS / 2f);
            float score = computeScore(loc);
            if (score > bestScore) {
                bestScore = score;
                bestLoc = loc;
            }
        }

        return bestLoc;
    }

    @Override
    public void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target) {
        if (target.getOriginalOwner() != 1 && target.getOwner() != -1) return;
        if (target.isFighter()) return;

        cooldownMult -= 0.015f;
        cooldownMult = Math.max(cooldownMult, 0.25f);
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        for (var entry : existingLocs.entrySet()) {
            var loc = entry.getKey();
            var data = entry.getValue();
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            float alpha = 0.3f - Math.max(0f, (3f - data.timeLeft)) * 0.3f;
            GL11.glColor4f(0.8f, 1f, 0.9f, alpha);
            GL11.glVertex2f(loc.x, loc.y);

            GL11.glColor4f(0.6f, 1f, 0.8f, 0f);
            for (float angle = 0f; angle <= 360f; angle += 2f) {
                GL11.glVertex2f(
                        loc.x + data.radius*(float)Math.cos(angle*Misc.RAD_PER_DEG),
                        loc.y + data.radius*(float)Math.sin(angle*Misc.RAD_PER_DEG));
            }
            GL11.glEnd();

            if (data.timeLeft >= data.totalTime/2f) {
                float mult = Math.min(1f, Math.min(data.totalTime - data.timeLeft, data.timeLeft - data.totalTime/2f));
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                GL11.glColor4f(0.5f, 1f, 0.75f, 0.15f*mult);
                GL11.glVertex2f(loc.x, loc.y);

                GL11.glColor4f(0.7f, 1f, 0.8f, 0f);
                for (float angle = 0f; angle <= 360f; angle += 2f) {
                    GL11.glVertex2f(
                            loc.x + (RADIUS / 4f) * (float) Math.cos(angle * Misc.RAD_PER_DEG),
                            loc.y + (RADIUS / 4f) * (float) Math.sin(angle * Misc.RAD_PER_DEG));
                }
                GL11.glEnd();
            }
        }
        GL11.glDisable(GL11.GL_BLEND);
    }
}
