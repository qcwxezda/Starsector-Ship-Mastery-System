package shipmastery.combat.listeners;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.Set;

/** Attach to a ship or engine. Reports whenever *any* ship is destroyed. */
public interface ShipDestroyedListener {
    /** use target.getParamAboutToApplyDamage() to get the actual projectile or other object that destroyed the ship*/
    void reportShipDestroyed(Set<ShipAPI> recentlyDamagedBy, ShipAPI target);
}
