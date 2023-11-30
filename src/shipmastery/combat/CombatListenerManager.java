package shipmastery.combat;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.FleetMemberDeploymentListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.state.AppDriver;
import shipmastery.campaign.StateTracker;
import shipmastery.util.EngineUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class CombatListenerManager extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private final Set<String> playerShips = new HashSet<>();
    private final Set<String> enemyShips = new HashSet<>();
    private final ShipSystemTracker shipSystemTracker = new ShipSystemTracker();
    private final FlagshipTracker flagshipTracker = new FlagshipTracker();
    private final IntervalUtil updateInterval = new IntervalUtil(2f, 3f);

    @Override
    public void init(CombatEngineAPI engine) {
        // Could use a FleetMemberDeployedListener, but it doesn't track refit simulation ship or stations
        // (possibly all ships with modules?)
        this.engine = engine;
        engine.getListenerManager().addListener(flagshipTracker);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        StateTracker.setState(AppDriver.getInstance().getCurrentState().getID());

        if (engine == null || engine.isPaused()) return;

        List<ShipAPI> ships = engine.getShips();
        // Ship system listeners stored per ship
        shipSystemTracker.advance(ships, amount);
        // Flagship trackers stored in engine
        flagshipTracker.advance(engine);

        updateInterval.advance(amount);
        if (updateInterval.intervalElapsed()) {
            updateShipList(FleetSide.PLAYER);
            updateShipList(FleetSide.ENEMY);
        }
    }

    private void updateShipList(FleetSide side) {
        Set<String> list = side == FleetSide.PLAYER ? playerShips : enemyShips;
        for (DeployedFleetMemberAPI dfm : engine.getFleetManager(side).getDeployedCopyDFM()) {
            // We don't care about damage that fighters take
            if (dfm.isFighterWing() || dfm.getShip() == null) {
                continue;
            }
            ShipAPI ship = dfm.getShip();
            String shipId = ship.getId();
            if (!list.contains(shipId)) {
                list.add(shipId);
                ShipAPI baseShip = EngineUtils.getBaseShip(ship);
                // Note: DamageListener listens for damage taken only
                if (!ship.hasListenerOfClass(ShipDamageTracker.class)) {
                    ship.addListener(new ShipDamageTracker());
                }
            }
        }
    }
}
