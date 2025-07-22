package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DroneStrikeBoost extends ShipSystemEffect {

    @Override
    public MasteryDescription getDescription(ShipVariantAPI selectedVariant, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription
                .initDefaultHighlight(Strings.Descriptions.DroneStrikeBoost)
                .params(getSystemName(), Utils.asPercent(getStrength(selectedVariant)));
    }

    @Override
    public void applyEffectsAfterShipCreationIfHasSystem(ShipAPI ship) {
        if (!ship.hasListenerOfClass(DroneStrikeBoostScript.class)) {
            ship.addListener(new DroneStrikeBoostScript(ship, getStrength(ship),  id));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "drone_strike";
    }

    record DroneStrikeBoostScript(ShipAPI ship, float increase,
                                  String id) implements ProjectileCreatedListener {
        DroneStrikeBoostScript(ShipAPI ship, float increase, String id) {
            this.ship = ship;
            this.increase = increase;
            this.id = id;
            ship.getSystem().setFluxPerUse(0f);
        }

        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!"terminator_missile_proj".equals(proj.getProjectileSpecId())) return;
            MissileAPI missile = (MissileAPI) proj;
            missile.getDamage().getModifier().modifyPercent(id, 100f * increase);
            missile.getEngineStats().getMaxSpeed().modifyPercent(id, 100f * increase);
            missile.getEngineStats().getMaxTurnRate().modifyPercent(id, 100f * increase);
            missile.getEngineStats().getTurnAcceleration().modifyPercent(id, 100f * increase);
            missile.setHitpoints(missile.getHitpoints() + missile.getMaxHitpoints() * increase);
        }
    }
}
