package shipmastery.combat;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.state.AppDriver;
import shipmastery.campaign.StateTracker;
import shipmastery.util.Strings;

import java.util.List;

@SuppressWarnings("unused")
public class CombatListenerManager extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    /** Needs to be cleared on game load to prevent memory leak. */
    private static BattleCreationContext lastBattleCreationContext = null;
    private final ShipSystemTracker shipSystemTracker = new ShipSystemTracker();
    private final FlagshipTracker flagshipTracker = new FlagshipTracker();
    private final ProjectileTracker projectileTracker = new ProjectileTracker();
    private final IntervalUtil updateInterval = new IntervalUtil(2f, 3f);

    public static BattleCreationContext getLastBattleCreationContext() {
        return lastBattleCreationContext;
    }

    public static void clearLastBattleCreationContext() {
        lastBattleCreationContext = null;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        // Could use a FleetMemberDeployedListener, but it doesn't track refit simulation ship or stations
        // (possibly all ships with modules?)
        this.engine = engine;
        engine.getListenerManager().addListener(flagshipTracker);
        engine.getListenerManager().addListener(projectileTracker);
        projectileTracker.init(engine);
        lastBattleCreationContext = engine.getContext();

        if (lastBattleCreationContext != null) {
            var otherFleet = lastBattleCreationContext.getOtherFleet();
            if (otherFleet != null) {
                // Add plugin for nucleus defender fight
                String fleetType = otherFleet.getMemoryWithoutUpdate().getString(MemFlags.MEMORY_KEY_FLEET_TYPE);
                if (Strings.Campaign.NUCLEUS_DEFENDER_FLEET_TYPE.equals(fleetType)) {
                    engine.addPlugin(new NucleusDefenderHandler());
                }
                // Modify admiral AI for remote beacon fight
                if (Strings.Campaign.REMOTE_BEACON_DEFENDER_FLEET_TYPE.equals(fleetType)) {
                    RemoteBeaconDefenderHandler.modifyAdmiralAI(engine.getFleetManager(FleetSide.ENEMY), otherFleet.getFlagship());
                }
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        StateTracker.setState(AppDriver.getInstance().getCurrentState().getID());

        if (engine == null || engine.isPaused()) return;

        List<ShipAPI> ships = engine.getShips();
        // Ship system listeners stored per ship
        shipSystemTracker.advance(ships, amount);
        projectileTracker.advance(engine, amount);
        // Flagship trackers stored in engine
        flagshipTracker.advance(engine);

        updateInterval.advance(amount);
        if (updateInterval.intervalElapsed()) {
            updateShipList();
        }
    }

    private void updateShipList() {
        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isAlive()) continue;
            // Note: DamageListener listens for damage taken only
            if (!ship.hasListenerOfClass(ShipDamageTracker.class)) {
                ship.addListener(new ShipDamageTracker(ship));
            }
        }
    }
}
