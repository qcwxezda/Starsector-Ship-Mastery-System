package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class TargetingFeedBoost extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        float strength = getStrength(selectedModule);
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.TargetingFeedBoost)
                .params(systemName, Utils.asPercent(strength), Utils.asPercent(2f * strength));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"targetingfeed".equals(ship.getSystem().getId())) return;
        ship.getSystem().setFluxPerUse(0f);
        if (!ship.hasListenerOfClass(TargetingFeedBoostScript.class)) {
            ship.addListener(new TargetingFeedBoostScript(ship, getStrength(ship), 2f * getStrength(ship), id));
        }
    }

    static class TargetingFeedBoostScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float speedIncrease;
        final float damageToFightersIncrease;
        final String id;

        TargetingFeedBoostScript(ShipAPI ship, float speedIncrease, float damageToFightersIncrease, String id) {
            this.ship = ship;
            this.speedIncrease = speedIncrease;
            this.damageToFightersIncrease = damageToFightersIncrease;
            this.id = id;
        }

        @Override
        public void onFullyDeactivate() {
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null) continue;
                for (ShipAPI fighter : bay.getWing().getWingMembers()) {
                    fighter.getMutableStats().getMaxSpeed().unmodify(id);
                    fighter.getMutableStats().getDamageToFighters().unmodify(id);
                }
            }
        }

        @Override
        public void advanceWhileOn(float amount) {
            float effectLevel = ship.getSystem().getEffectLevel();
            for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
                if (bay.getWing() == null) continue;
                for (ShipAPI fighter : bay.getWing().getWingMembers()) {
                    fighter.getMutableStats().getMaxSpeed().modifyPercent(id, 100f * speedIncrease * effectLevel);
                    fighter.getMutableStats().getDamageToFighters().modifyPercent(id, 100f * damageToFightersIncrease * effectLevel);
                }
            }
            Utils.maintainStatusForPlayerShip(
                    ship,
                    id,
                    "graphics/icons/hullsys/targeting_feed.png",
                    Strings.Descriptions.TargetingFeedBoostTitle,
                    String.format(Strings.Descriptions.TargetingFeedBoostDesc1, Utils.asPercentNoDecimal(speedIncrease)),
                    false);
            Utils.maintainStatusForPlayerShip(
                    ship,
                    id + "2",
                    "graphics/icons/hullsys/targeting_feed.png",
                    Strings.Descriptions.TargetingFeedBoostTitle,
                    String.format(Strings.Descriptions.TargetingFeedBoostDesc2, Utils.asPercentNoDecimal(damageToFightersIncrease)),
                    false);
        }
    }
}
