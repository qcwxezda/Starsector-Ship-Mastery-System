package shipmastery.mastery.impl;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

@SuppressWarnings("unused")
public class EmptyMastery extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.EmptyMastery);
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        return null;
    }

    @Override
    public float getNPCWeight(FleetMemberAPI fm) {
        return 0f;
    }
}
