package shipmastery.ui.triggers;

import shipmastery.util.ClassRefs;

public abstract class ActionListener extends TriggerableProxy {
    public ActionListener() {
        super(ClassRefs.actionListenerInterface, "actionPerformed");
    }
}
