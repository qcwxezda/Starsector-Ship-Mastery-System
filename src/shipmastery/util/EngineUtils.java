package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

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

    public final static float maxRangeUseGrid = 300f;
    public static Collection<CombatEntityAPI> getKNearestEntities(
            int k,
            Vector2f location,
            ShipAPI.HullSize smallestToNote,
            boolean includeMissiles,
            float maxRange,
            final boolean considerRadius,
            TargetChecker checker) {

        CombatEngineAPI engine = Global.getCombatEngine();
        // second entry is distance to location
        List<Pair<CombatEntityAPI, Float>> shipsAndMissiles = new ArrayList<>();

        if (maxRange <= maxRangeUseGrid) {
            Iterator<Object> itr = engine.getAllObjectGrid().getCheckIterator(location, 2f*maxRange, 2f*maxRange);
            while (itr.hasNext()) {
                Object o = itr.next();
                if (o instanceof ShipAPI) {
                    ShipAPI ship = (ShipAPI) o;
                    float dist = getDistWithEntity(location, ship, considerRadius);
                    if (dist <= maxRange && checker.check(ship) && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                        shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(ship, dist));
                    }
                }
                else if (o instanceof MissileAPI && includeMissiles) {
                    MissileAPI missile = (MissileAPI) o;
                    float dist = getDistWithEntity(location, missile, considerRadius);
                    if (dist <= maxRange && checker.check(missile)) {
                        shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(missile, dist));
                    }
                }
            }
        }
        else {
            for (ShipAPI ship : engine.getShips()) {
                float dist = getDistWithEntity(location, ship, considerRadius);
                if (dist <= maxRange && checker.check(ship) && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                    shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(ship, dist));
                }
            }
            if (includeMissiles) {
                for (MissileAPI missile : engine.getMissiles()) {
                    float dist = getDistWithEntity(location, missile, considerRadius);
                    if (dist <= maxRange && checker.check(missile)) {
                        shipsAndMissiles.add(new Pair<CombatEntityAPI, Float>(missile, dist));
                    }
                }
            }
        }
        Collections.sort(shipsAndMissiles, new Comparator<Pair<CombatEntityAPI, Float>>() {
            @Override
            public int compare(Pair<CombatEntityAPI, Float> p1, Pair<CombatEntityAPI, Float> p2) {
                return Float.compare(p1.two, p2.two);
            }
        });

        List<CombatEntityAPI> kNearest = new ArrayList<>();
        for (int i = 0; i < Math.min(k, shipsAndMissiles.size()); i++) {
            kNearest.add(shipsAndMissiles.get(i).one);
        }

        return kNearest;
    }

    /** Can be negative for points inside the entity */
    public static float getDistWithEntity(Vector2f location, CombatEntityAPI entity, boolean considerRadius) {
        return Misc.getDistance(location, entity.getLocation()) - (considerRadius ? entity.getCollisionRadius() : 0f);
    }

    /** If the argument is a ship, returns that ship.
     *  If the argument is a wing, returns the wing's source ship.
     *  If the argument is a module, returns the module's base ship/station. */
    public static ShipAPI getBaseShip(ShipAPI shipWingOrModule) {
        if (shipWingOrModule == null) {
            return null;
        }
        // The "ship" in question is a drone
        if (shipWingOrModule.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP)) {
            return getBaseShip((ShipAPI) shipWingOrModule.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP));
        }
        if (shipWingOrModule.isFighter()) {
            ShipAPI base = null;
            if (shipWingOrModule.getWing() == null ||
                    shipWingOrModule.getWing().getSourceShip() == null) {
                // If the fighter has no source ship but has a fleet member,
                // just return the fighter itself
                if (shipWingOrModule.getFleetMember() != null) {
                    base = shipWingOrModule;
                }
            }
            else {
                base = getBaseShip(shipWingOrModule.getWing().getSourceShip());
            }
            return base;
        }
        if (shipWingOrModule.isStationModule()) {
            ShipAPI base = null;
            if (shipWingOrModule.getParentStation() == null) {
                // If the module has no parent station but has a fleet member,
                // just return the module itself
                if (shipWingOrModule.getFleetMember() != null) {
                    base = shipWingOrModule;
                }
            }
            else {
                base = getBaseShip(shipWingOrModule.getParentStation());
            }
            return base;
        }
        return shipWingOrModule;
    }

    public static boolean isFighter(CombatEntityAPI entity) {
        return entity instanceof ShipAPI && ((ShipAPI) entity).isFighter();
    }
}
