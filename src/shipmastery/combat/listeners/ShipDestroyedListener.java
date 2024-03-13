package shipmastery.combat.listeners;

import com.fs.starfarer.api.combat.ShipAPI;

public interface ShipDestroyedListener {
    /** use target.getParamAboutToApplyDamage() to get the actual projectile or other object that destroyed the ship*/
    void reportShipDestroyed(ShipAPI source, ShipAPI target);
}
