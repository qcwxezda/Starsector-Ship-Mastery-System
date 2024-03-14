package shipmastery.mastery.impl.combat;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import shipmastery.mastery.BaseMasteryEffect;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ExtraFighterPerWing extends BaseMasteryEffect {

    public static final int MIN_FIGHTERS_PER_WING = 3;

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.init(Strings.Descriptions.ExtraFighterPerWing).params(MIN_FIGHTERS_PER_WING).colors(
                Misc.getTextColor());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ExtraFighterPerWingPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (!ship.hasListenerOfClass(ExtraFighterPerWingScript.class)) {
            ship.addListener(new ExtraFighterPerWingScript(ship));
        }
    }

    static class ExtraFighterPerWingScript implements AdvanceableListener {

        final ShipAPI ship;
        final IntervalUtil extraDeploymentChecker = new IntervalUtil(1f, 1f);

        ExtraFighterPerWingScript(ShipAPI ship) {
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
                    int numInWing = spec.getNumFighters();
                    if (numInWing < MIN_FIGHTERS_PER_WING) continue;
                    if (bay.getExtraDeployments() > 0) continue;
                    if (bay.getWing().getWingMembers().size() >= numInWing + 1) continue;
                    bay.setExtraDeploymentLimit(numInWing + 1);
                    bay.setExtraDeployments(1);
                    bay.setExtraDuration(999999999f);
                }
            }
        }
    }

    @Override
    public Float getSelectionWeight(ShipHullSpecAPI spec) {
        if (spec.isCivilianNonCarrier()) return null;
        if (spec.getFighterBays() <= 0) return null;
        return Utils.getSelectionWeightScaledByValue(spec.getFighterBays(), 2, false);
    }
}
