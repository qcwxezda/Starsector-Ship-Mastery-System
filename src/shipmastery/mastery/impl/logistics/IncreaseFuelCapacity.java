package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class IncreaseFuelCapacity extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return Utils.makeGenericNegatableDescription(getMult(), Strings.INCREASE_FUEL_CAPACITY, Strings.INCREASE_FUEL_CAPACITY_NEG, true);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFuelMod().modifyMult(id, 1f + getMult());
    }

    public float getMult() {
        return Math.max(-1f, 0.1f * getStrength());
    }
}
