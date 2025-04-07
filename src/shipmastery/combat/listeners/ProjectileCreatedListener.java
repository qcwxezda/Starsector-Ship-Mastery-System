package shipmastery.combat.listeners;


import com.fs.starfarer.api.combat.DamagingProjectileAPI;

/** Attach to a ship. This listener then reports whenever the ship shoots a projectile. */
public interface ProjectileCreatedListener {
    void reportProjectileCreated(DamagingProjectileAPI proj);
}
