package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.ReserveWingStats;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.deferred.Action;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class ReserveDeploymentBoost extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.ReserveDeploymentBoost).params(
                Utils.asInt(getCount(selectedModule)), systemName, Utils.asPercent(getRefitTimeDecrease(selectedModule)));
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI selectedModule,
                                          FleetMemberAPI selectedFleetMember) {
        tooltip.addPara(Strings.Descriptions.ReserveDeploymentBoostPost, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"reservewing".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(ReserveDeploymentBoostScript.class)) {
            ship.addListener(new ReserveDeploymentBoostScript(ship, getCount(ship), getRefitTimeDecrease(ship), id));
        }
    }

    public int getCount(ShipAPI ship) {
        return (int) getStrength(ship);
    }

    public float getRefitTimeDecrease(ShipAPI ship) {
        return getStrength(ship) * 0.25f / 3.5f;
    }

    static class ReserveDeploymentBoostScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final int count;
        final float refitTimeDecrease;
        final String id;

        ReserveDeploymentBoostScript(ShipAPI ship, int count, float refitTimeDecrease, String id) {
            this.ship = ship;
            this.count = count;
            this.refitTimeDecrease = refitTimeDecrease;
            this.id = id;
        }

        @Override
        public void onActivate() {
            CombatDeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                        bay.setExtraDeployments(bay.getExtraDeployments() + count);
                    }
                    ship.getMutableStats().getFighterRefitTimeMult().modifyMult(id, 1f - refitTimeDecrease);
                }
            }, 1f);

            CombatDeferredActionPlugin.performLater(new Action() {
                @Override
                public void perform() {
                    ship.getMutableStats().getFighterRefitTimeMult().unmodify(id);
                }
            }, ReserveWingStats.EXTRA_FIGHTER_DURATION);
        }
    }
}
