package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;

public abstract class EngineUtils {
    /** Uses the collision grid. For small maxRange values. Set smallestToNote to null to include missiles. */

    public static boolean isEntityNearby(
            Vector2f location,
            ShipAPI.HullSize smallestToNote,
            float maxShipRange,
            float maxMissileRange,
            boolean considerCollisionRadius,
            TargetChecker checker) {
        float maxRange = Math.max(maxShipRange, maxMissileRange);
        Iterator<Object>
                itr = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(location, 2f*maxRange, 2f*maxRange);
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (!(obj instanceof CombatEntityAPI)) continue;

            CombatEntityAPI entity = (CombatEntityAPI) obj;
            if (!checker.check(entity)) {
                continue;
            }

            float dist = Misc.getDistance(location, entity.getLocation()) - (entity.getCollisionRadius() * (considerCollisionRadius ? 1f : 0f));

            if (dist <= maxMissileRange && entity instanceof MissileAPI && smallestToNote == null) {
                return true;
            }

            if (entity instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) entity;
                if (dist <= maxShipRange && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** If the argument is a ship, returns that ship.
     *  If the argument is a module, returns the module's base ship/station. */
    public static ShipAPI getBaseShip(ShipAPI shipOrModule) {
        if (shipOrModule == null) {
            return null;
        }
        if (shipOrModule.isStationModule()) {
            ShipAPI base = null;
            if (shipOrModule.getParentStation() == null) {
                // If the module has no parent station but has a fleet member,
                // just return the module itself
                if (shipOrModule.getFleetMember() != null) {
                    base = shipOrModule;
                }
            }
            else {
                base = getBaseShip(shipOrModule.getParentStation());
            }
            return base;
        }
        return shipOrModule;
    }

    public static boolean isFighter(CombatEntityAPI entity) {
        return entity instanceof ShipAPI && ((ShipAPI) entity).isFighter();
    }
}
