package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class SModCapacity extends AdditiveMasteryEffect {

    @Override
    public MasteryDescription getDescription() {
        return MasteryDescription
                .init(Strings.Descriptions.SModCapacity)
                .params(getIncrease())
                .colors(Misc.getHighlightColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getIncrease());
    }
}