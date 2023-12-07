package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class RegroupReplacement extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        String str = Utils.asPercent(getStrength(selectedModule));
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.RegroupReplacement).params(str, str);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(RegroupReplacementScript.class)) {
            ship.addListener(new RegroupReplacementScript(ship, getStrength(ship), id));
        }
    }

    static class RegroupReplacementScript implements AdvanceableListener {
        final ShipAPI ship;
        final float strength;
        final String id;

        RegroupReplacementScript(ShipAPI ship, float strength, String id) {
            this.ship = ship;
            this.strength = strength;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            if (ship.isPullBackFighters()) {
                ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(id, strength);
                ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyPercent(id, 100f * strength);
            }
            else {
                ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_DECREASE_MULT).unmodify(id);
                ship.getMutableStats().getDynamic().getStat(Stats.REPLACEMENT_RATE_INCREASE_MULT).unmodify(id);
            }
        }
    }
}
