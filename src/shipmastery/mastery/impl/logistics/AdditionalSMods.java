package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class AdditionalSMods extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription() {
        int count = getCount();
        return MasteryDescription
                .init(count == 1 ? Strings.ADDITIONAL_SMOD_DESCRIPTION_SINGLE : Strings.ADDITIONAL_SMOD_DESCRIPTION_PLURAL)
                .params(count)
                .colors(Misc.getHighlightColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getCount());
    }

    int getCount() {
        return Math.max(1, (int) getStrength());
    }

    @Override
    public boolean canBeDeactivated() {
        return false;
    }
}
