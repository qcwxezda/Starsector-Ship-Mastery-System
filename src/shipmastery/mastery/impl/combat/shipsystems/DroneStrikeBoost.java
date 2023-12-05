package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;

public class DroneStrikeBoost extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.DroneStrikeBoost)
                .params(systemName, Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"drone_strike".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(DroneStrikeBoostScript.class)) {
            // numMissiles should be getNumToFire, but the method is protected, so...
            ship.addListener(new DroneStrikeBoostScript(ship, getStrength(ship), 1, id));
        }
    }

    static class DroneStrikeBoostScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float increase;
        final String id;
        final int numMissiles;

        DroneStrikeBoostScript(ShipAPI ship, float increase, int numMissiles, String id) {
            this.ship = ship;
            this.increase = increase;
            this.numMissiles = numMissiles;
            this.id = id;
        }

        @Override
        public void onActivate() {
            ArrayList<DamagingProjectileAPI> projectiles = (ArrayList<DamagingProjectileAPI>) Global.getCombatEngine().getProjectiles();
            // Find the last n created missiles by this ship, they should have terminator_missile_proj as projectile id
            int numProcessed = 0;
            for (int i = projectiles.size() - 1; i >= 0; i--) {
                DamagingProjectileAPI proj = projectiles.get(i);
                if (proj.getSource() == ship && "terminator_missile_proj".equals(proj.getProjectileSpecId())) {
                    // Set some custom data just to make sure we don't process the same missile more than once
                    if (proj.getCustomData() == null || !proj.getCustomData().containsKey(id)) {
                        proj.setCustomData(id, true);

                        MissileAPI missile = (MissileAPI) proj;
                        missile.getDamage().getModifier().modifyPercent(id, 100f * increase);
                        missile.getEngineStats().getMaxSpeed().modifyPercent(id, 100f * increase);
                        missile.getEngineStats().getMaxTurnRate().modifyPercent(id, 100f * increase);
                        missile.getEngineStats().getTurnAcceleration().modifyPercent(id, 100f * increase);
                        missile.setHitpoints(missile.getHitpoints() + missile.getMaxHitpoints() * increase);

                        numProcessed++;
                        if (numProcessed >= numMissiles) {
                            break;
                        }
                    }
                }
            }
        }
    }
}
