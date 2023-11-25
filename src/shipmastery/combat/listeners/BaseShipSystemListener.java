package shipmastery.combat.listeners;

import com.fs.starfarer.api.combat.ShipAPI;

public abstract class BaseShipSystemListener implements ShipSystemListener {
    @Override
    public void onActivate(ShipAPI ship) {}

    @Override
    public void onDeactivate(ShipAPI ship) {}

    @Override
    public void onFullyActivate(ShipAPI ship) {}

    @Override
    public void onFullyDeactivate(ShipAPI ship) {}

    @Override
    public void onGainedAmmo(ShipAPI ship) {}

    @Override
    public void onFullyCharged(ShipAPI ship) {}

    @Override
    public void advanceWhileOn(ShipAPI ship, float amount) {}
}
