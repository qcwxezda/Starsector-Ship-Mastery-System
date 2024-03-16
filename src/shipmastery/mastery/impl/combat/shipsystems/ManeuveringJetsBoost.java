package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ManeuveringJetsBoost extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ManeuveringJetsBoost).params(
                getSystemName(), Utils.asInt(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(ManeuveringJetsBoostScript.class)) {
            ship.addListener(new ManeuveringJetsBoostScript(ship, getStrength(ship), id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "maneuveringjets";
    }

    static class ManeuveringJetsBoostScript extends BaseShipSystemListener {

        final ShipAPI ship;
        final float boost;
        final String id;

        ManeuveringJetsBoostScript(ShipAPI ship, float boost, String id) {
            this.ship = ship;
            this.boost = boost;
            this.id = id;
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getMaxSpeed().unmodify(id);
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel();
            ship.getMutableStats().getMaxSpeed().modifyFlat(id, boost * effectLevel);
            Utils.maintainStatusForPlayerShip(
                    ship,
                    id,
                    "graphics/icons/hullsys/maneuvering_jets.png",
                    Strings.Descriptions.ManeuveringJetsBoostTitle,
                    String.format(Strings.Descriptions.ManeuveringJetsBoostDesc1, Utils.asInt(boost * effectLevel)),
                    false);
        }
    }
}
