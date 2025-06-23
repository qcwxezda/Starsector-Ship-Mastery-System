package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class MaxCRFloor extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MaxCRFloor).params(Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        //MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getMaxCombatReadiness().unmodify(id);
        float maxCR = stats.getMaxCombatReadiness().getModifiedValue();
        // Why does basic maintenance get computed after hullmods?
        maxCR += 0.7f;
        float str = getStrength(stats);
        if (maxCR < str) {
            stats.getMaxCombatReadiness().modifyFlat(id, str - maxCR, Strings.Misc.shipMasteryEffect);
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (!spec.isBuiltInMod(HullMods.AUTOMATED)) return null;
        return 1f;
    }
}
