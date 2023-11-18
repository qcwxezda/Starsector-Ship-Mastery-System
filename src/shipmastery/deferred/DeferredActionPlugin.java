package shipmastery.deferred;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

import java.util.PriorityQueue;
import java.util.Queue;

/** Uses system time because game time seems to run faster inside a market dialog */
public class DeferredActionPlugin implements EveryFrameScript {

    final Queue<DeferredAction> actionList = new PriorityQueue<>();
    public static final String INSTANCE_KEY = "$shipmastery_DeferredActionPlugin";

    public static void performLater(Action action, float delay) {
        DeferredActionPlugin instance = getInstance();
        if (instance != null) {
            instance.actionList.add(new DeferredAction(action, System.currentTimeMillis() + (long) (1000f*delay)));
        }
    }

    public static DeferredActionPlugin getInstance() {
        return (DeferredActionPlugin) Global.getSector().getMemory().get(INSTANCE_KEY);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        DeferredAction firstItem;
        while ((firstItem = actionList.peek()) != null && firstItem.timeToPerform <= System.currentTimeMillis()) {
            actionList.poll();
            firstItem.action.perform();
        }

    }

}
