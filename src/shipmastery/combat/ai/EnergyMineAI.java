package shipmastery.combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import shipmastery.util.EngineUtils;
import shipmastery.util.TargetChecker;

import java.awt.*;

public class EnergyMineAI implements ProximityFuseAIAPI, MissileAIPlugin {

    private final MissileAPI mine;
    private final TargetChecker checker;

    public EnergyMineAI(MissileAPI mine) {
        this.mine = mine;
        checker = new TargetChecker.CommonChecker(mine);
    }

    @Override
    public void advance(float amount) {
        if (!mine.didDamage() && (mine.isFading()
                || EngineUtils.isEntityNearby(mine.getLocation(), ShipAPI.HullSize.FIGHTER, 250f, 0f, false, checker))) {
            explode();
        }
    }

    private void explode() {
        DamagingExplosionSpec spec = new DamagingExplosionSpec(
                0.1f,
                400f,
                250f,
                mine.getDamageAmount(),
                mine.getDamageAmount() / 2,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                4f,
                3f,
                0.8f,
                100,
                Color.CYAN,
                Color.BLUE
        );

        spec.setUseDetailedExplosion(true);
        spec.setDetailedExplosionRadius(400);
        spec.setDetailedExplosionFlashRadius(600);
        spec.setDetailedExplosionFlashColorCore(Color.WHITE);
        spec.setDetailedExplosionFlashColorFringe(Color.ORANGE);
        spec.setDamageType(DamageType.ENERGY);
        Global.getCombatEngine().spawnDamagingExplosion(spec, mine.getSource(), mine.getLocation());
        Global.getCombatEngine().removeEntity(mine);
    }

    @Override
    public void updateDamage() {}
}
