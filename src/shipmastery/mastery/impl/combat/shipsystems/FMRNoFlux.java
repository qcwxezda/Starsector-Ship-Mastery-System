package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class FMRNoFlux extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.FMRNoFlux).params(getSystemName());
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        ship.getSystem().setFluxPerUse(0f);
    }

    @Override
    public String getSystemSpecId() {
        return "fastmissileracks";
    }
}
