package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;

public abstract class ShipSystemEffect extends BaseMasteryEffect {
    String name;
    public String getSystemName() {
        if (name != null) return name;
        return name = Global.getSettings().getShipSystemSpec(getSystemSpecId()).getName();
    }

    public abstract String getSystemSpecId();

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getShipSystemId() == null || !spec.getShipSystemId().equals(getSystemSpecId())) return null;
        return 3f;
    }
}
