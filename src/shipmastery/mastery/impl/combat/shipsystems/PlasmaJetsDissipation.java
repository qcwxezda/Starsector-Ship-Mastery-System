package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class PlasmaJetsDissipation extends ShipSystemEffect {


    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.PlasmaJetsDissipation)
                                 .params(systemName, Utils.asFloatTwoDecimals(1f + strength), Utils.asPercent(strength));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"plasmajets".equals(ship.getSystem().getId())) {
            return;
        }
        if (!ship.hasListenerOfClass(PlasmaJetsDissipationScript.class)) {
            ship.addListener(new PlasmaJetsDissipationScript(ship, getStrength(ship), id));
        }
    }

    static class PlasmaJetsDissipationScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float strength;
        final String id;
        PlasmaJetsDissipationScript(ShipAPI ship, float strength, String id) {
            this.ship = ship;
            this.strength = strength;
            this.id = id;
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getFluxDissipation().unmodify(id);
            ship.getMutableStats().getHardFluxDissipationFraction().unmodify(id);
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel();
            ship.getMutableStats().getFluxDissipation().modifyMult(id, 1f + strength*effectLevel);
            ship.getMutableStats().getHardFluxDissipationFraction().modifyFlat(id, strength*effectLevel);
        }
    }
}
