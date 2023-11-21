package shipmastery.combat.listeners;

import com.fs.starfarer.api.combat.ShipAPI;

public interface FlagshipListener {
    void playerFlagshipChanged(ShipAPI from, ShipAPI to);

    void enemyFlagshipChanged(ShipAPI from, ShipAPI to);
}
