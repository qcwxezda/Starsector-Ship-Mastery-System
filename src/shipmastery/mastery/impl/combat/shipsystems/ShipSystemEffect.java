package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipSystemSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;

public abstract class ShipSystemEffect extends BaseMasteryEffect {

    protected ShipSystemSpecAPI systemSpec;
    protected String systemName = "";
    protected String systemSpriteName = "";

    @Override
    public void init(String... args) {
        super.init(args);
        systemSpec = Global.getSettings().getShipSystemSpec(getHullSpec().getShipSystemId());
        systemName = systemSpec.getName();
        systemSpriteName = systemSpec.getIconSpriteName();
    }
}
