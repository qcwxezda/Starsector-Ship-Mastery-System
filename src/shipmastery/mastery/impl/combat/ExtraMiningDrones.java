package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ExtraMiningDrones extends BaseMasteryEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.ExtraMiningDrones)
                .params(Utils.asInt(getStrength(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ExtraMiningDronesPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(ExtraMiningDronesScript.class)) {
            ship.addListener(new ExtraMiningDronesScript((int) getStrength(ship), ship));
        }
    }

    static class ExtraMiningDronesScript implements AdvanceableListener {

        final ShipAPI ship;
        final int extraDrones;
        final IntervalUtil extraDeploymentChecker = new IntervalUtil(1f, 1f);

        ExtraMiningDronesScript(int extraDrones, ShipAPI ship) {
            this.extraDrones = extraDrones;
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            // The effect has lower priority than other effects that add extra deployments
            // check every second whether the extra deployment duration is zero
            // if that's the case, go ahead and add the permanent extra deployment
            extraDeploymentChecker.advance(amount);
            if (extraDeploymentChecker.intervalElapsed()) {
                for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                    if (bay.getWing() == null) continue;
                    FighterWingSpecAPI spec = bay.getWing().getSpec();
                    if (!"mining_drone_wing".equals(spec.getId())) continue;
                    int numInWing = spec.getNumFighters();
                    if (bay.getExtraDeployments() > 0) continue;
                    if (bay.getWing().getWingMembers().size() >= numInWing + extraDrones) continue;
                    bay.setExtraDeploymentLimit(numInWing + extraDrones);
                    bay.setExtraDeployments(1);
                    bay.setExtraDuration(999999999f);
                }
            }
        }
    }
}
