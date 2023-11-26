package shipmastery.fx;

import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class StaticRingEmitter extends BaseIEmitter {

    final Vector2f location;
    final float radius;
    final Color color;
    final float sizeJitterFrac;
    final SpriteAPI sprite = particleengine.Utils.getLoadedSprite("graphics/fx/shields256ringc.png");

    public StaticRingEmitter(Vector2f location, float radius, Color color, float sizeJitterFrac) {
        this.location = location;
        this.radius = radius;
        this.color = color;
        this.sizeJitterFrac = sizeJitterFrac;
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
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.size(2f*radius*MathUtils.randBetween(1f-sizeJitterFrac, 1f+sizeJitterFrac), 2f*radius*MathUtils.randBetween(1f-sizeJitterFrac, 1f+sizeJitterFrac));
        data.life(0.5f);
        data.fadeTime(0.25f, 0.25f);
        data.color(color);
        return data;
    }
}
