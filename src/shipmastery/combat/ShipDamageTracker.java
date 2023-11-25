package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import shipmastery.combat.listeners.ShipDestroyedListener;

public class ShipDamageTracker implements DamageListener {
    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!(source instanceof ShipAPI) || !(target instanceof ShipAPI)) return;
        if (target.getHitpoints() <= 0f) {
            // CombatListenerUtil in base game copies the listener list when calling reportDamageApplied, so this is safe
            ((ShipAPI) target).removeListener(this);
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (ship.hasListenerOfClass(ShipDestroyedListener.class)) {
                    for (ShipDestroyedListener listener : ship.getListeners(ShipDestroyedListener.class)) {
                        listener.reportShipDestroyed((ShipAPI) source, (ShipAPI) target, result);
                    }
                }
            }
        }
    }
}
