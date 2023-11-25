package shipmastery.combat.listeners;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;

public interface ShipDestroyedListener {
    void reportShipDestroyed(ShipAPI source, ShipAPI target, ApplyDamageResultAPI lastDamageResult);
}
