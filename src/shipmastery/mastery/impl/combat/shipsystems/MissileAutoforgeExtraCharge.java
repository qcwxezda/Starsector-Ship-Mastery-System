package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class MissileAutoforgeExtraCharge extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MissileAutoforgeExtraCharge).params(getSystemName());
    }

    @Override
    public void applyEffectsBeforeShipCreationIfHasSystem(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        stats.getSystemUsesBonus().modifyFlat(id, 1);
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        ship.getSystem().setFluxPerUse(0f);
    }

    @Override
    public String getSystemSpecId() {
        return "forgevats";
    }
}
