package shipmastery.combat.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

public abstract class AdvanceIfAliveListener implements AdvanceableListener {
    protected ShipAPI ship;
    public AdvanceIfAliveListener(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public final void advance(float amount) {
        if (!Global.getCombatEngine().isShipAlive(ship)) {
            ship.removeListener(this);
            return;
        }
        advanceIfAlive(amount);
    }

    public abstract void advanceIfAlive(float amount);
}
