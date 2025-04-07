package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.ProjectileCreatedListener;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

public class DecoyFlareBoost extends ShipSystemEffect{
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.DecoyFlareBoost)
                                 .params(getSystemName(), Utils.asPercent(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !getSystemSpecId().equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(DecoyFlareBoostScript.class)) {
            ship.addListener(new DecoyFlareBoostScript(ship, getStrength(ship)));
        }
    }

    @Override
    public String getSystemSpecId() {
        return "flarelauncher_fighter";
    }

    record DecoyFlareBoostScript(ShipAPI ship, float strength) implements ProjectileCreatedListener {
        @Override
            public void reportProjectileCreated(DamagingProjectileAPI proj) {
                if (!(proj instanceof MissileAPI missile)) return;
                if (!"flare_fighter".equals(missile.getProjectileSpecId())) return;
                missile.setHitpoints(missile.getMaxHitpoints() * (1f + strength));
                missile.setMaxFlightTime(missile.getMaxFlightTime() * (1f + strength));
            }
        }
}
