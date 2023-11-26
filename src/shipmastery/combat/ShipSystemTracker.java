package shipmastery.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import shipmastery.combat.listeners.ShipSystemListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShipSystemTracker {
    private final Map<ShipAPI, StateData> stateLastFrameMap = new HashMap<>();
    void advance(List<ShipAPI> ships, float amount) {
        for (ShipAPI ship : ships) {
            List<ShipSystemListener> listeners = ship.getListeners(ShipSystemListener.class);
            if (listeners.isEmpty() || ship.getSystem() == null) continue;

            if (!stateLastFrameMap.containsKey(ship)) {
                stateLastFrameMap.put(ship, new StateData(false, false, false, ship.getSystem().getMaxAmmo()));
            }

            ShipSystemAPI system = ship.getSystem();
            StateData prevData = stateLastFrameMap.get(ship);
            boolean wasActive = prevData.isActive;
            boolean wasOn = prevData.isOn;
            boolean wasChargeUp = prevData.isChargeUp;
            int prevAmmo = prevData.ammo;

            for (ShipSystemListener listener : listeners) {
                if (system.isActive()) {
                    if (!wasActive) {
                        listener.onActivate();
                    }
                    listener.advanceWhileOn(amount);
                }

                if (!system.isChargeup() && wasChargeUp) {
                    listener.onFullyActivate();
                }

                if (!system.isOn() && wasOn) {
                    listener.onDeactivate();
                }

                if (!system.isActive() && wasActive) {
                    listener.onFullyDeactivate();
                }

                if (prevAmmo < system.getAmmo()) {
                    listener.onGainedAmmo();

                    if (system.getAmmo() == system.getMaxAmmo()) {
                        listener.onFullyCharged();
                    }
                }
            }

            stateLastFrameMap.put(ship, new StateData(system.isOn(), system.isActive(), system.isChargeup(), system.getAmmo()));
        }
    }

    private static class StateData {
        boolean isOn;
        boolean isActive;
        boolean isChargeUp;
        int ammo;
        private StateData(boolean isOn, boolean isActive, boolean isChargeUp, int ammo) {
            this.isOn = isOn;
            this.isActive = isActive;
            this.isChargeUp = isChargeUp;
            this.ammo = ammo;
        }
    }
}
