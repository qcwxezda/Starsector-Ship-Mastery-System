package shipmastery.mastery.impl.combat.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import shipmastery.mastery.MasteryDescription;
import shipmastery.util.Strings;
import shipmastery.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class CanisterFlakWhileVenting extends ShipSystemEffect {
    @Override
    public MasteryDescription getDescription(ShipAPI selectedModule, FleetMemberAPI selectedFleetMember) {
        return MasteryDescription.initDefaultHighlight(Strings.Descriptions.CanisterFlakWhileVenting).params(
                systemName, Utils.asFloatOneDecimal(getStrength(selectedModule)));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship) {
        if (ship.getSystem() == null || !"canister_flak".equals(ship.getSystem().getId())) return;
        if (!ship.hasListenerOfClass(CanisterFlakWhileVentingScript.class)) {
            ship.addListener(new CanisterFlakWhileVentingScript(ship, getStrength(ship)));
        }
    }

    static class CanisterFlakWhileVentingScript implements AdvanceableListener {
        final ShipAPI ship;
        final List<WeaponAPI> canisterFlaks = new ArrayList<>();
        final List<IntervalUtil> flakIntervals = new ArrayList<>();

        CanisterFlakWhileVentingScript(ShipAPI ship, float rate) {
            this.ship = ship;

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if ("canister_flak".equals(weapon.getId())) {
                    canisterFlaks.add(weapon);
                    flakIntervals.add(new IntervalUtil(0.8f / rate, 1.25f / rate));
                }
            }
        }
        @Override
        public void advance(float amount) {
            if (!ship.getFluxTracker().isVenting() || canisterFlaks.isEmpty()) return;
            for (int i = 0; i < canisterFlaks.size(); i++) {
                WeaponAPI weapon = canisterFlaks.get(i);
                IntervalUtil flakInterval = flakIntervals.get(i);
                flakInterval.advance(amount);
                if(flakInterval.intervalElapsed()) {
                    MissileAPI missile = (MissileAPI) Global.getCombatEngine()
                                                            .spawnProjectile(ship, weapon, weapon.getId(),
                                                                             weapon.getFirePoint(0),
                                                                             weapon.getCurrAngle(), ship.getVelocity());
                    if (missile.getSpec().getOnFireEffect() != null) {
                        missile.getSpec().getOnFireEffect().onFire(missile, weapon, Global.getCombatEngine());
                    }

                    Global.getCombatEngine().spawnMuzzleFlashOrSmoke(ship, weapon.getFirePoint(0), weapon.getSpec(), weapon.getCurrAngle());
                    Global.getSoundPlayer().playSound("system_canister_flak_fire", 1f, 1f, weapon.getFirePoint(0), ship.getVelocity());
                }
            }
        }
    }
}
