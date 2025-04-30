package shipmastery.deferred;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class CombatDeferredActionPlugin extends BaseEveryFrameCombatPlugin {
    private float currentTime;
    private CombatEngineAPI engine;
    public final static String customDataKey = "shipmastery_CombatDeferredActionPlugin";
    private final Queue<DeferredAction> actionList = new PriorityQueue<>();

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        currentTime = 0f;
        actionList.clear();

        engine.getCustomData().put(customDataKey, this);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        DeferredAction firstItem;
        while ((firstItem = actionList.peek()) != null && firstItem.timeToPerform <= currentTime * 1000f) {
            actionList.poll();
            firstItem.action.perform();
        }

        currentTime += amount;
    }

    public static void performLater(Action action, float delay) {
        CombatDeferredActionPlugin instance = getInstance();
        if (instance != null) {
            instance.actionList.add(new DeferredAction(action, (long) ((instance.currentTime + delay) * 1000f)));
        }
    }

    public static CombatDeferredActionPlugin getInstance() {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return null;
        }

        return (CombatDeferredActionPlugin) engine.getCustomData().get(customDataKey);
    }
}
