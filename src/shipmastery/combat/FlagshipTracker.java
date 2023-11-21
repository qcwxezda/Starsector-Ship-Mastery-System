package shipmastery.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import shipmastery.combat.listeners.FlagshipListener;

import java.util.List;

public class FlagshipTracker {
    ShipAPI trackedPlayerFlagship;
    ShipAPI trackedEnemyFlagship;
    void advance(CombatEngineAPI engine, List<ShipAPI> ships, float amount) {
        ShipAPI playerFlagship = engine.getPlayerShip();
        CombatFleetManagerAPI enemyFleetManager = engine.getFleetManager(FleetSide.ENEMY);
        ShipAPI enemyFlagship = enemyFleetManager.getShipFor(enemyFleetManager.getFleetCommander());

        if (playerFlagship != trackedPlayerFlagship) {
            trackedPlayerFlagship = playerFlagship;
            for (ShipAPI ship : ships) {
                List<FlagshipListener> listeners = ship.getListeners(FlagshipListener.class);
                for (FlagshipListener listener : listeners) {
                    listener.playerFlagshipChanged(trackedPlayerFlagship, playerFlagship);
                }
            }
        }

        if (enemyFlagship != trackedEnemyFlagship) {
            trackedEnemyFlagship = enemyFlagship;
            for (ShipAPI ship : ships) {
                List<FlagshipListener> listeners = ship.getListeners(FlagshipListener.class);
                for (FlagshipListener listener : listeners){
                    listener.enemyFlagshipChanged(trackedEnemyFlagship, enemyFlagship);
                }
            }
        }
    }
}
