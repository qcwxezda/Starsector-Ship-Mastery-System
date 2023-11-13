package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Utils;

public class AdditionalSMods extends BaseMasteryEffect {

    static final String DESCRIPTION_STR_SINGLE = Utils.getString("sms_descriptions", "additionalSModsSingle");
    static final String DESCRIPTION_STR_PLURAL = Utils.getString("sms_descriptions", "additionalSModsPlural");

    @Override
    public MasteryDescription getDescription() {
        int count = getCount();
        return new MasteryDescription(
                count == 1 ? DESCRIPTION_STR_SINGLE : DESCRIPTION_STR_PLURAL,
                new Object[] {count},
                Misc.getHighlightColor());
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, getCount());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip) {}

    @Override
    public void addTooltip(TooltipMakerAPI tooltip) {}

    @Override
    public boolean hasTooltip() {
        return false;
    }

    @Override
    public boolean hideUntilUnlocked() {
        return true;
    }

    int getCount() {
        return Math.max(1, (int) getStrength());
    }
}
