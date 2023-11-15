package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class AdditionalSMods extends BaseMasteryEffect {

    @Override
    public MasteryDescription getDescription(ShipHullSpecAPI spec) {
        int count = getCount();
        return MasteryDescription
                .init(Strings.ADDITIONAL_SMOD_DESCRIPTION)
                .params(count)
                .colors(Misc.getHighlightColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getCount());
    }

    public int getCount() {
        return 1 + (int) ((getStrength() - 1) * 0.2f);
    }
}
