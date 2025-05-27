package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DamperFieldDissipation extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.DamperFieldDissipation)
                                 .params(getSystemName(), Utils.asFloatTwoDecimals(1f + getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(DamperFieldDissipationScript.class)) {
            ship.addListener(new DamperFieldDissipationScript(ship, getStrength(ship)));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "damper";
    }

    class DamperFieldDissipationScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float strength;

        DamperFieldDissipationScript(ShipAPI ship, float strength) {
            this.ship = ship;
            this.strength = strength;
        }

        @Override
        public void onFullyDeactivate() {
            ship.getMutableStats().getFluxDissipation().unmodify(id);
        }

        @Override
        public void advanceWhileOn(float amount) {
            float mult = 1f + strength * ship.getSystem().getEffectLevel();
            ship.getMutableStats().getFluxDissipation().modifyMult(id, mult);
            Utils.maintainStatusForPlayerShip(
                    ship,
                    id,
                    getSystemSpec().getIconSpriteName(),
                    Strings.Descriptions.DamperFieldDissipationTitle,
                    String.format(Strings.Descriptions.DamperFieldDissipationDesc1, Utils.asFloatTwoDecimals(mult)),
                    false);
        }
    }
}
