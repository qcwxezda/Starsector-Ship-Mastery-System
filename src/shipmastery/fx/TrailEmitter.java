package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class TrailEmitter extends BaseIEmitter {
    final DamagingProjectileAPI proj;
    public float life = 1f, fadeOut = 0.25f, lifeJitter = 0f;
    public float width = 10f, length = 25f, sizeJitter = 0f, yOffset = 0f;
    public Color color = Color.WHITE;

    public DamagingProjectileAPI getProj() {
        return proj;
    }

    @Override
    public Vector2f getLocation() {
        return proj.getLocation();
    }

    @Override
    public float getXDir() {
        return proj.getFacing() - 90f;
    }

    public TrailEmitter(DamagingProjectileAPI proj) {
        this.proj = proj;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return CombatEngineLayers.CONTRAILS_LAYER;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.offset(new Vector2f(0f, yOffset));
        data.size(
                width * MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter),
                length * MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter));
        data.life(life * MathUtils.randBetween(1f - lifeJitter, 1f + lifeJitter));
        data.fadeTime(0f, fadeOut);
        data.color(color);
        return data;
    }
}
