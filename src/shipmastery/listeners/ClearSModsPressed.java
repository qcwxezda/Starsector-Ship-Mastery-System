package shipmastery.listeners;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.ui.ButtonAPI;

import java.util.ArrayList;
import java.util.HashSet;

public class ClearSModsPressed extends ActionListener{

    MasteryButtonPressed parentListener;
    public ClearSModsPressed(MasteryButtonPressed parentListener) {
        this.parentListener = parentListener;
    }

    @Override
    public void trigger(Object... args) {
        ButtonAPI button = (ButtonAPI) args[1];
        if ((boolean) button.getCustomData()) {
            ShipVariantAPI variant = parentListener.getShip().getVariant();
            // Copy required as removePermaMod also calls getSMods().remove()
            for (String id : new ArrayList<>(variant.getSMods())) {
                variant.removePermaMod(id);
            }

            parentListener.forceRefresh();

            // Some non-smodded hullmods may no longer be applicable; remove these also
            for (String id : variant.getNonBuiltInHullmods()) {
                HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
                if (!spec.getEffect().isApplicableToShip(parentListener.getShip())) {
                    variant.removeMod(id);
                }
            }

            parentListener.forceRefresh();
        }
        else {
            button.setCustomData(true);
        }
    }
}
