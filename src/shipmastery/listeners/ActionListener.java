package shipmastery.listeners;

import shipmastery.util.ClassRefs;

public abstract class ActionListener extends ProxyTrigger {
    public ActionListener() {
        super(ClassRefs.actionListenerInterface, "actionPerformed");
    }
}
