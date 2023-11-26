package shipmastery.combat.listeners;

/** onActivate (effect level 0) -> onFullyActivate (effect level 1) -> onDeactivate (effect level 1) -> onFullyDeactivate (effect level 0)
 *  Listener is ship specific -- attached to a ship */
public interface ShipSystemListener {
    void onActivate();

    void onDeactivate();

    void onFullyActivate();

    void onFullyDeactivate();

    void onGainedAmmo();

    void onFullyCharged();

    void advanceWhileOn(float amount);
}
