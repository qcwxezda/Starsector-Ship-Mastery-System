package shipmastery.fx;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Pair;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class GlowEmitter extends BaseIEmitter {

    public final Vector2f location;
    public Color color = Color.WHITE;
    public float startSize = 100f, endSize = 100f, maxSize = 100f;
    public float life = 1f;
    public SpriteAPI sprite;
    public float fadeInFrac = 0.05f, fadeOutFrac = 0.95f;

    private float growthRate = 0f, growthAcceleration = 0f, savedStartSize = 100f, savedEndSize = 100f, savedMaxSize = 100f;

    public GlowEmitter(Vector2f location) {
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

    public void updateSize() {
        Pair<Float, Float> rAndA = MathUtils.getRateAndAcceleration(startSize, endSize, maxSize, life);
        growthRate = rAndA.one;
        growthAcceleration = rAndA.two;
        savedStartSize = startSize;
        savedEndSize = endSize;
        savedMaxSize = maxSize;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.size(startSize, startSize);

        if (savedStartSize != startSize || savedMaxSize != maxSize || savedEndSize != endSize) {
            updateSize();
        }

        data.growthRate(growthRate, growthRate);
        data.growthAcceleration(growthAcceleration, growthAcceleration);
        data.life(life);
        data.fadeTime(life * fadeInFrac, life * fadeOutFrac);
        data.color(color);
        return data;
    }
}
