package shipmastery.deferred;

public class DeferredAction implements Comparable<DeferredAction> {
    Action action;
    long timeToPerform;

    public DeferredAction(Action action, long time) {
        this.action = action;
        timeToPerform = time;
    }

    @Override
    public int compareTo(DeferredAction other) {
        return Long.compare(timeToPerform, other.timeToPerform);
    }
}
