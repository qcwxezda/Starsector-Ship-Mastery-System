package shipmastery.listeners;

import com.fs.starfarer.api.combat.ShipAPI;
import shipmastery.util.ReflectionUtils;

public class MasteryButtonPressed extends ActionListener {

    ShipAPI ship;

    public MasteryButtonPressed(ShipAPI ship) {
        this.ship = ship;
    }

    @Override
    public void trigger(Object... args) {
        ReflectionUtils.showGenericDialog("This is a generic dialog!", "Finish", 800f, 600f);
    }
}
