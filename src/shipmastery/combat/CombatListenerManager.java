package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.state.AppDriver;
import shipmastery.campaign.StateTracker;

import java.util.List;

@SuppressWarnings("unused")
public class CombatListenerManager extends BaseEveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private final ShipSystemTracker shipSystemTracker = new ShipSystemTracker();
    private final FlagshipTracker flagshipTracker = new FlagshipTracker();

    @Override
    public void init(CombatEngineAPI engine) {
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
    }
}
