package shipmastery.ui.triggers;

import shipmastery.util.ClassRefs;

public abstract class DialogDismissedListener extends TriggerableProxy {
    public DialogDismissedListener() {
        super(ClassRefs.dialogDismissedInterface, "dialogDismissed");
    }
}
