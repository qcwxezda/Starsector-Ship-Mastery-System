package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;

public class MissileAutoforgeExtraCharge extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.MissileAutoforgeExtraCharge).params(systemName);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats) {
        if (stats.getVariant() == null || stats.getVariant().getHullSpec() == null || !"forgevats".equals(stats.getVariant().getHullSpec().getShipSystemId())) return;
        stats.getSystemUsesBonus().modifyFlat(id, 1);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        ship.getSystem().setFluxPerUse(0f);
    }
}
