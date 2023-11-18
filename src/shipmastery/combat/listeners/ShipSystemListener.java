package shipmastery.combat.listeners;


import com.fs.starfarer.api.combat.ShipAPI;

/** onActivate (effect level 0) -> onFullyActivate (effect level 1) -> onDeactivate (effect level 1) -> onFullyDeactivate (effect level 0) */
public interface ShipSystemListener {
    void onActivate(ShipAPI ship);

    void onDeactivate(ShipAPI ship);

    void onFullyActivate(ShipAPI ship);

    void onFullyDeactivate(ShipAPI ship);

    void onGainedAmmo(ShipAPI ship);

    void onFullyCharged(ShipAPI ship);

    void advanceWhileOn(ShipAPI ship, float amount);
}
