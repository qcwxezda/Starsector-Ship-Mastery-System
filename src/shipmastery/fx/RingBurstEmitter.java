package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

public class RingBurstEmitter extends BaseIEmitter {

    final Vector2f center;
    final float startRadius, endRadius, resolution;

    public RingBurstEmitter(Vector2f center, float startRadius, float endRadius, float resolution) {
        this.center = center;
        this.startRadius = startRadius;
        this.endRadius = endRadius;
        this.resolution = resolution;
    }

    @Override
    public Vector2f getLocation() {
        return center;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return CombatEngineLayers.ABOVE_SHIPS_LAYER;
    }

    @Override
    protected ParticleData initParticle(int i) {
        float theta = (float) i;
        float baseLife = 0.75f;
        float life = baseLife * MathUtils.randBetween(0.95f, 1.05f);
        Vector2f locDir = Misc.getUnitVectorAtDegreeAngle(theta);
        Vector2f loc = new Vector2f(startRadius*locDir.x, startRadius*locDir.y);
        ParticleData data = new ParticleData();
        data.offset(loc);
        data.life(life);
        data.velocity(new Vector2f(locDir.x * (endRadius - startRadius) / baseLife * MathUtils.randBetween(0.95f, 1.05f), locDir.y * MathUtils.randBetween(0.95f, 1.05f) * (endRadius - startRadius) / baseLife));
        data.fadeTime(life / 4f, life / 2f);
        data.size(100f, 100f);
        data.growthRate(endRadius * 0.25f / life * MathUtils.randBetween(0.8f, 1.2f), endRadius / life * MathUtils.randBetween(0.5f, 1.5f));
        data.revolutionRate(MathUtils.randBetween(-10f, 10f));
        data.facing(theta - 90f + MathUtils.randBetween(-30f, 30f));
        data.color(0.7f, 1f, 0.85f, 0.125f);
        data.saturationShift(MathUtils.randBetween(0f, 0.5f));
        return data;
    }
}
