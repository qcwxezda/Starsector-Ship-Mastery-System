package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ReduceFuelUsage extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return Utils.makeGenericNegatableDescription(getMult(), Strings.REDUCE_FUEL_USAGE, Strings.REDUCE_FUEL_USAGE_NEG, true);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFuelUseMod().modifyMult(id, 1f - getMult());
    }

    public float getMult() {
        return Math.min(1f, 0.1f * getStrength());
    }
}
