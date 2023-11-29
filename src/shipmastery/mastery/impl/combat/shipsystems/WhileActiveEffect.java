package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;

public abstract class WhileActiveEffect extends BaseMasteryEffect {

    protected ShipSystemSpecAPI systemSpec;
    @Override
    public void init(String... args) {
        super.init(args);
        systemSpec = Global.getSettings().getShipSystemSpec(getHullSpec().getShipSystemId());
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        ShipSystemSpecAPI system = Global.getSettings().getShipSystemSpec(spec.getShipSystemId());
        if (system == null) return null;
        // System must be active for at least one second to count as an active system
        if (system.getActive() < 1f && !system.isToggle()) return null;
        return 1f;
    }

}
