package shipmastery.fx;

import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class ParticleBurstEmitter extends BaseIEmitter {

    final Vector2f location;
    public float radius = 100f, radiusJitter = 0f;
    public float life = 1f, lifeJitter = 0f;
    public float size = 10f, sizeJitter = 0f;
    public float alpha = 1f, alphaJitter = 0f;
    public Color color = Color.WHITE;

    @Override
    public Vector2f getLocation() {
        return location;
    }

    public ParticleBurstEmitter(Vector2f location) {
        this.location = location;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        float newLife = life * MathUtils.randBetween(1f - lifeJitter, 1f + lifeJitter);
        float theta = MathUtils.randBetween(0f, 360f);
        Vector2f velocity = Misc.getUnitVectorAtDegreeAngle(theta);
        float velocityScale = MathUtils.randBetween(1f - radiusJitter, 1f + radiusJitter);
        velocity.scale(radius / newLife * velocityScale);
        data.life(newLife);
        float newSize = size * MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter);
        data.size(newSize * (1f + velocityScale), newSize);
        data.velocity(velocity);
        Color newColor = new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alpha * MathUtils.randBetween(1f - alphaJitter, 1f + alphaJitter));
        data.color(newColor);
        data.fadeTime(0f, newLife / 2f);
        data.facing(theta);
        return data;
    }
}
