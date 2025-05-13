package shipmastery.fx;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class StaticRingEmitter extends BaseIEmitter {

    final Vector2f location;
    public float startRadius = 0f, endRadius = 100f;
    public Color color = Color.WHITE;
    public float sizeJitterFrac = 0f;
    public float life = 1f;
    final SpriteAPI sprite = particleengine.Utils.getLoadedSprite("graphics/fx/sms_tex_ring_highres.png");

    public StaticRingEmitter(Vector2f location) {
        this.location = location;
    }

    @Override
    public Vector2f getLocation() {
        return location;
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public float getRenderRadius() {
        return endRadius;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        float jitter = MathUtils.randBetween(1f-sizeJitterFrac, 1f+sizeJitterFrac);
        data.size(2f*startRadius*jitter, 2f*startRadius*MathUtils.randBetween(1f-sizeJitterFrac, 1f+sizeJitterFrac)*jitter);
        data.growthRate(2f*(endRadius - startRadius)/life, 2f*(endRadius - startRadius)/life);
        data.life(life);
        data.fadeTime(life*0.2f, life*0.8f);
        data.color(color);
        return data;
    }
}
