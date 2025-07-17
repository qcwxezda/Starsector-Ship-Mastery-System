package shipmastery.util;

import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.combat.listeners.AdvanceIfAliveListener;

public abstract class StackableBuffWithExpiry extends AdvanceIfAliveListener {

    protected float strength = 0f;
    protected float durationRemaining = 0f;
    protected final float fadeOutTime;

    public StackableBuffWithExpiry(ShipAPI ship, float fadeOutTime) {
        super(ship);
        this.fadeOutTime = fadeOutTime;
    }

    public abstract void unapplyEffects();

    public abstract void applyEffects(float fadeOutFrac);

    @Override
    public final void advanceIfAlive(float amount) {
        durationRemaining -= amount;
        if (durationRemaining <= -fadeOutTime) {
            unapplyEffects();
            ship.removeListener(this);
            return;
        }

        float effectStrength = 1f;
        if (durationRemaining < 0f) {
            effectStrength += durationRemaining / fadeOutTime;
        }

        applyEffects(1f - effectStrength);
    }

    public final void addStack(float stackStrength, float maxStrength, float durationToKeep) {
        if (strength > maxStrength) return;
        strength = Math.min(maxStrength, strength + stackStrength);
        durationRemaining = Math.max(durationToKeep, durationRemaining);
    }

    public static void addStackToShip(StackableBuffWithExpiry newScript, ShipAPI ship, float stackStrength, float maxStrength, float durationToKeep) {
        var listeners = ship.getListeners(newScript.getClass());
        if (listeners.isEmpty()) {
            newScript.addStack(stackStrength, maxStrength, durationToKeep);
            ship.addListener(newScript);
        } else {
            listeners.forEach(x -> x.addStack(stackStrength, maxStrength, durationToKeep));
        }
    }
}
