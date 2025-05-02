package shipmastery.deferred;

import java.io.Serializable;

public interface Action extends Serializable {
    void perform();
}
