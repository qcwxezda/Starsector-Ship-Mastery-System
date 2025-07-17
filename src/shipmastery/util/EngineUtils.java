package shipmastery.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.EndOfCombatListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            if (!(obj instanceof CombatEntityAPI entity)) continue;

            if (!checker.check(entity)) {
                continue;
            }

            float dist = Misc.getDistance(location, entity.getLocation()) - (entity.getCollisionRadius() * (considerCollisionRadius ? 1f : 0f));

            if (dist <= maxMissileRange && entity instanceof MissileAPI && smallestToNote == null) {
                return true;
            }

            if (entity instanceof ShipAPI ship) {
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
                if (o instanceof ShipAPI ship) {
                    float dist = getDistWithEntity(location, ship, considerRadius);
                    if (dist <= maxRange && checker.check(ship) && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                        shipsAndMissiles.add(new Pair<>(ship, dist));
                    }
                }
                else if (o instanceof MissileAPI missile && includeMissiles) {
                    float dist = getDistWithEntity(location, missile, considerRadius);
                    if (dist <= maxRange && checker.check(missile)) {
                        shipsAndMissiles.add(new Pair<>(missile, dist));
                    }
                }
            }
        }
        else {
            for (ShipAPI ship : engine.getShips()) {
                float dist = getDistWithEntity(location, ship, considerRadius);
                if (dist <= maxRange && checker.check(ship) && (smallestToNote == null || ship.getHullSize().compareTo(smallestToNote) >= 0)) {
                    shipsAndMissiles.add(new Pair<>(ship, dist));
                }
            }
            if (includeMissiles) {
                for (MissileAPI missile : engine.getMissiles()) {
                    float dist = getDistWithEntity(location, missile, considerRadius);
                    if (dist <= maxRange && checker.check(missile)) {
                        shipsAndMissiles.add(new Pair<>(missile, dist));
                    }
                }
            }
        }
        shipsAndMissiles.sort((p1, p2) -> Float.compare(p1.two, p2.two));

        List<CombatEntityAPI> kNearest = new ArrayList<>();
        for (int i = 0; i < Math.min(k, shipsAndMissiles.size()); i++) {
            kNearest.add(shipsAndMissiles.get(i).one);
        }

        return kNearest;
    }

    /** The Misc version requires a ShipAPI as the anchor location, this can take an arbitrary anchor point */
    public static ShipAPI getClosestEntity(
            Vector2f location,
            ShipAPI.HullSize smallestToNote,
            float maxRange,
            boolean considerShipRadius,
            TargetChecker checker) {
        ShipAPI closest = null;
        float closestDist = Float.MAX_VALUE;
        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (!checker.check(ship) || (smallestToNote != null && ship.getHullSize().compareTo(smallestToNote) < 0)) {
                continue;
            }

            float dist = Misc.getDistance(location, ship.getLocation());
            if (dist <= maxRange + (ship.getCollisionRadius() * (considerShipRadius ? 1f : 0f))
                    && dist < closestDist) {
                closest = ship;
                closestDist = dist;
            }
        }
        return closest;
    }

    /** Can be negative for points inside the entity */
    public static float getDistWithEntity(Vector2f location, CombatEntityAPI entity, boolean considerRadius) {
        return Misc.getDistance(location, entity.getLocation()) - (considerRadius ? entity.getCollisionRadius() : 0f);
    }

    /** Returns true iff {@code ship} is equal to {@code owner}, is a fighter launched by {@code owner}, or is a drone with mothership {@code owner}. */
    public static boolean shipIsOwnedBy(ShipAPI ship, ShipAPI owner) {
        if (ship == owner) return true;
        if (ship.isFighter() && ship.getWing() != null && ship.getWing().getSourceShip() == owner) return true;
        return ship.getAIFlags() != null &&
                ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP) == owner;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shipOrOwnerInSet(Set<ShipAPI> set, ShipAPI ship) {
        for (ShipAPI source : set) {
            if (EngineUtils.shipIsOwnedBy(source, ship)) {
                return true;
            }
        }
        return false;
    }

    private static final Map<ShipAPI, ShipAPI> BASE_SHIP_CACHE = new HashMap<>();

    public static class ClearCacheOnCombatEnd implements EndOfCombatListener {
        @Override
        public void onCombatEnd() {
            BASE_SHIP_CACHE.clear();
        }
    }

    /** If the argument is a ship, returns that ship.
     *  If the argument is a wing, returns the wing's source ship.
     *  If the argument is a module, returns the module's base ship/station. */
    public static ShipAPI getBaseShip(ShipAPI shipWingOrModule) {
        return getBaseShip(shipWingOrModule, new HashSet<>());
    }

    private static ShipAPI getBaseShip(ShipAPI shipWingOrModule, Set<ShipAPI> seen) {
        if (shipWingOrModule == null) {
            return null;
        }

        var cached = BASE_SHIP_CACHE.get(shipWingOrModule);
        if (cached != null) {
            return cached;
        }

        if (seen.contains(shipWingOrModule)) {
            BASE_SHIP_CACHE.put(shipWingOrModule, shipWingOrModule);
            // Early exit to prevent infinite loop, this should never happen though
            // as ships shouldn't be parent modules of themselves or their own parent modules, etc.
            return shipWingOrModule;
        }
        seen.add(shipWingOrModule);
        // The "ship" in question is a drone
        if (shipWingOrModule.getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP)) {
            var base = getBaseShip((ShipAPI) shipWingOrModule.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP), seen);
            BASE_SHIP_CACHE.put(shipWingOrModule, base);
            return base;
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
                base = getBaseShip(shipWingOrModule.getWing().getSourceShip(), seen);
            }
            BASE_SHIP_CACHE.put(shipWingOrModule, base);
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
                base = getBaseShip(shipWingOrModule.getParentStation(), seen);
            }
            BASE_SHIP_CACHE.put(shipWingOrModule, base);
            return base;
        }
        BASE_SHIP_CACHE.put(shipWingOrModule, shipWingOrModule);
        return shipWingOrModule;
    }

    public static boolean isFighter(CombatEntityAPI entity) {
        return entity instanceof ShipAPI && ((ShipAPI) entity).isFighter();
    }

    /** Includes the ship itself */
    public static List<ShipAPI> getAllModules(ShipAPI ship) {
        List<ShipAPI> list = new ArrayList<>();
        if (ship == null) return list;
        getAllModulesHelper(ship, list);
        return list;
    }

    private static void getAllModulesHelper(ShipAPI ship, List<ShipAPI> list) {
        list.add(ship);
        ship.getChildModulesCopy().forEach(child -> getAllModulesHelper(child, list));
    }
}
