package shipmastery.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.CombatEngine;
import shipmastery.combat.listeners.ProjectileCreatedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Notifies when projectiles get spawned */
public class ProjectileTracker {
    private List<DamagingProjectileAPI> projectileList;
    private final Set<DamagingProjectileAPI> seenProjectiles = new HashSet<>();
    private final IntervalUtil seenCleanupInterval = new IntervalUtil(1f, 2f);

    public void init(CombatEngineAPI engine) {
        //noinspection unchecked
        projectileList = (List<DamagingProjectileAPI>) ((CombatEngine) engine).getObjects().getList(DamagingProjectileAPI.class);
    }

    public void advance(CombatEngineAPI engine, float amount) {
        Map<ShipAPI, List<DamagingProjectileAPI>> newProjectiles = new HashMap<>();

        for (int i = projectileList.size() - 1; i >= 0; i--) {
            DamagingProjectileAPI proj = projectileList.get(i);
            if (seenProjectiles.contains(proj)) {
                break;
            }
            if (proj.getSource() != null) {
                newProjectiles.computeIfAbsent(proj.getSource(), k -> new ArrayList<>()).add(proj);
            }
            seenProjectiles.add(proj);
        }

        for (Map.Entry<ShipAPI, List<DamagingProjectileAPI>> entry : newProjectiles.entrySet()) {
            ShipAPI ship = entry.getKey();
            for (ProjectileCreatedListener listener : ship.getListeners(ProjectileCreatedListener.class)) {
                for (DamagingProjectileAPI proj : entry.getValue()) {
                    listener.reportProjectileCreated(proj);
                }
            }
        }

        seenCleanupInterval.advance(amount);
        if (seenCleanupInterval.intervalElapsed()) {
            seenProjectiles.removeIf(entity -> !engine.isEntityInPlay(entity));
        }
    }
}
