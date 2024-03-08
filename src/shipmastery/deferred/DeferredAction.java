package shipmastery.deferred;

public class DeferredAction implements Comparable<DeferredAction> {
    final Action action;
    final long timeToPerform;

    public DeferredAction(Action action, long time) {
        this.action = action;
        timeToPerform = time;
    }

    @Override
    public int compareTo(DeferredAction other) {
        return Long.compare(timeToPerform, other.timeToPerform);
    }
}
