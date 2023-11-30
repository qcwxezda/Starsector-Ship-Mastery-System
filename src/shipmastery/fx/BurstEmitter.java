package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;

import java.awt.Color;

public class BurstEmitter extends BaseIEmitter {
    public SpriteAPI sprite;
    public Vector2f location = new Vector2f();
    public float xDir = 0f;
    public Color color;
    public float width;
    public float duration;
    public int numDirections;
    public float alphaMult = 1f;
    public float jitterRadius = 0f;
    public float widthGrowth = 0f;
    public int blendDestFactor = GL11.GL_ONE;

    public CombatEngineLayers layer = CombatEngineLayers.BELOW_SHIPS_LAYER;

    public BurstEmitter(SpriteAPI sprite, Color color, int numDirections, float width, float duration) {
        this.sprite = sprite;
        this.color = color;
        this.width = width;
        this.duration = duration;
        this.numDirections = numDirections;
    }

    @Override
    public Vector2f getLocation() {
        if (jitterRadius <= 0f) {
            return location;
        }
        Vector2f loc = Misc.getUnitVectorAtDegreeAngle(Misc.random.nextFloat() * 360f);
        loc.scale(Misc.random.nextFloat() * jitterRadius);
        Vector2f.add(loc, location, loc);
        return loc;
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }
    @Override
    public float getXDir() {
        return xDir;
    }

    @Override
    public int getBlendDestinationFactor() {
        return blendDestFactor;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return layer;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.size(sprite.getWidth(), sprite.getHeight());
        float theta = (Misc.random.nextFloat() + i) * 360f / numDirections;
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(theta);
        Vector2f velocity = new Vector2f(offset);
        offset.scale(width);
        velocity.scale(widthGrowth / duration);
        data.offset(offset);
        data.velocity(velocity);
        data.life(duration);
        data.fadeTime(duration / 4f, duration / 4f);
        data.color(new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f * alphaMult));
        return data;
    }
}