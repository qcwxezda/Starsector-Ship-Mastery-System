package shipmastery.combat.listeners;

public abstract class BaseShipSystemListener implements ShipSystemListener {
    @Override
    public void onActivate() {}

    @Override
    public void onDeactivate() {}

    @Override
    public void onFullyActivate() {}

    @Override
    public void onFullyDeactivate() {}

    @Override
    public void onGainedAmmo() {}

    @Override
    public void onFullyCharged() {}

    @Override
    public void advanceWhileOn(float amount) {}
}
