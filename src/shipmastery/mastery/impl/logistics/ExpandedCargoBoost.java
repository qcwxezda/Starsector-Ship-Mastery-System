package shipmastery.mastery.impl.logistics;

import com.fs.starfarer.api.Global;
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

public class ExpandedCargoBoost extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ExpandedCargoBoost).params(
                Global.getSettings().getHullModSpec(HullMods.EXPANDED_CARGO_HOLDS).getDisplayName(),
                Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null) return;
        if (stats.getVariant().hasHullMod(HullMods.EXPANDED_CARGO_HOLDS)) {
            stats.getCargoMod().modifyPercent(id, 100f * getStrength(stats));
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (!spec.isCivilianNonCarrier()) return null;
        return Utils.getSelectionWeightScaledByValueIncreasing(spec.getCargo(), 100f, 500f, 1500f);
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }
}
