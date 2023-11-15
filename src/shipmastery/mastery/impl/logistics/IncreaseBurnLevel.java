package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class IncreaseBurnLevel extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return MasteryDescription.initDefaultHighlight(Strings.INCREASE_BURN_LEVEL).params(getAmount());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getMaxBurnLevel().modifyFlat(id, getAmount());
    }

    public int getAmount() {
        return 1 + (int) ((getStrength() - 1)  * 0.25f);
    }
}
