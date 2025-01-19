package shipmastery.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.util.vector.Vector2f;
import shipmastery.combat.listeners.EMPEmitterDamageListener;
import shipmastery.combat.listeners.ShipDestroyedListener;
import shipmastery.deferred.Action;
import shipmastery.deferred.CombatDeferredActionPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShipDamageTracker implements DamageTakenModifier {
//    @Override
//    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
//        if (!(source instanceof ShipAPI) || !(target instanceof ShipAPI)) return;
//        ShipAPI sourceShip = (ShipAPI) source;
//        ShipAPI targetShip = (ShipAPI) target;
//        if ("EMP_SHIP_SYSTEM_PARAM".equals(targetShip.getParamAboutToApplyDamage())) {
//            for (EMPEmitterDamageListener listener : sourceShip.getListeners(EMPEmitterDamageListener.class)) {
//                listener.reportEMPEmitterHit(targetShip);
//            }
//        }
//        if (target.getHitpoints() <= 0f) {
//            // CombatListenerUtil in base game copies the listener list when calling reportDamageApplied, so this is safe
//            targetShip.removeListener(this);
//            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
//                if (ship.hasListenerOfClass(ShipDestroyedListener.class)) {
//                    for (ShipDestroyedListener listener : ship.getListeners(ShipDestroyedListener.class)) {
//                        listener.reportShipDestroyed((ShipAPI) source, (ShipAPI) target);
//                    }
//                }
//            }
//        }
//    }

    public static final float RECENTLY_DAMAGED_TIME = 1f;
    private final Map<ShipAPI, Set<ShipAPI>> recentlyDamagedBy = new HashMap<>();

    private void setRecentlyDamagedBy(final ShipAPI source, final ShipAPI target) {
        Set<ShipAPI> sources = recentlyDamagedBy.get(target);
        if (sources == null) {
            sources = new HashSet<>();
            recentlyDamagedBy.put(target, sources);
        }
        sources.add(source);
        CombatDeferredActionPlugin.performLater(new Action() {
            @Override
            public void perform() {
                removeRecentlyDamagedBy(source, target);
            }
        }, RECENTLY_DAMAGED_TIME);
    }

    private void removeRecentlyDamagedBy(ShipAPI source, ShipAPI target) {
        Set<ShipAPI> sources = recentlyDamagedBy.get(target);
        if (sources != null) {
            sources.remove(source);
            if (sources.isEmpty()) {
                recentlyDamagedBy.remove(target);
            }
        }
    }

    private @NotNull Set<ShipAPI> getRecentlyDamagedBy(ShipAPI target) {
        Set<ShipAPI> sources = recentlyDamagedBy.get(target);
        return sources == null ? new HashSet<ShipAPI>() : sources;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f pt,
                                    boolean shieldHit) {
        if (damage.getStats() == null) return null;
        Object source = damage.getStats().getEntity();
        if (!(source instanceof ShipAPI) || !(target instanceof ShipAPI)) return null;
        ShipAPI sourceShip = (ShipAPI) source;
        ShipAPI targetShip = (ShipAPI) target;
        if ("EMP_SHIP_SYSTEM_PARAM".equals(targetShip.getParamAboutToApplyDamage())) {
            for (EMPEmitterDamageListener listener : sourceShip.getListeners(EMPEmitterDamageListener.class)) {
                listener.reportEMPEmitterHit(targetShip, damage, pt, shieldHit);
            }
        }

        if (targetShip.isStationModule()) return null;

        setRecentlyDamagedBy(sourceShip, targetShip);
        if (target.getHitpoints() <= 0f) {
            // CombatListenerUtil in base game copies the listener list when calling reportDamageApplied, so this is safe
            targetShip.removeListenerOfClass(ShipDamageTracker.class);
            for (ShipAPI ship : Global.getCombatEngine().getShips()) {
                if (ship.hasListenerOfClass(ShipDestroyedListener.class)) {
                    for (ShipDestroyedListener listener : ship.getListeners(ShipDestroyedListener.class)) {
                        listener.reportShipDestroyed(getRecentlyDamagedBy(targetShip), targetShip);
                    }
                }
            }
        }
        return null;
    }
}
