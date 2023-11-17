package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.AdditiveMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class AdditionalSMods extends AdditiveMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        return MasteryDescription
                .init(Strings.ADDITIONAL_SMOD_DESCRIPTION)
                .params(getIncrease())
                .colors(Misc.getHighlightColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getIncrease());
    }
}
