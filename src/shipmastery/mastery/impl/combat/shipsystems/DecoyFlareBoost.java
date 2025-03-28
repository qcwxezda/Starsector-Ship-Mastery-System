package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import shipmastery.combat.listeners.BaseShipSystemListener;
import shipmastery.deferred.CombatDeferredActionPlugin;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.Iterator;

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

    static class DecoyFlareBoostScript extends BaseShipSystemListener {
        final ShipAPI ship;
        final float strength;

        DecoyFlareBoostScript(ShipAPI ship, float strength) {
            this.ship = ship;
            this.strength = strength;
        }

        @Override
        public void onActivate() {
            CombatDeferredActionPlugin.performLater(() -> {
                Iterator<Object> itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(ship.getLocation(), 200f, 200f);
                while (itr.hasNext()) {
                    Object o = itr.next();
                    if (!(o instanceof MissileAPI missile)) continue;
                    if (!"flare_fighter".equals(missile.getProjectileSpecId())) continue;
                    missile.setHitpoints(missile.getMaxHitpoints() * (1f + strength));
                    missile.setMaxFlightTime(missile.getMaxFlightTime() * (1f + strength));
                }
            }, 0.5f);
        }
    }
}
