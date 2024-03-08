package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class TrailEmitter extends BaseIEmitter {
    final DamagingProjectileAPI proj;
    public float life = 1f;
    public final float fadeOut = 0.25f;
    public float lifeJitter = 0f;
    public float width = 10f, length = 25f, sizeJitter = 0f, yOffset = 0f;
    public float randomAngleDegrees = 0f;
    public float saturationChangeOverLife = 0f;
    public float randomXOffset = 0f;
    public Color color = Color.WHITE;
    public SpriteAPI sprite;

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
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return CombatEngineLayers.CONTRAILS_LAYER;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.offset(new Vector2f(MathUtils.randBetween(-randomXOffset / 2f, randomXOffset / 2f), yOffset));
        data.size(
                width * MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter),
                length * MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter));
        float newLife = life * MathUtils.randBetween(1f - lifeJitter, 1f + lifeJitter);
        data.life(newLife);
        data.facing(MathUtils.randBetween(-randomAngleDegrees / 2f, randomAngleDegrees / 2f));
        data.saturationShift(saturationChangeOverLife / newLife);
        data.fadeTime(0f, fadeOut);
        data.color(color);
        return data;
    }
}
