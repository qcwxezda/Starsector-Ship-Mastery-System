package shipmastery.fx;

import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;
import shipmastery.util.Utils;

import java.awt.Color;

public class FireEmitter extends BaseIEmitter {

    public Vector2f location;
    public SpriteAPI sprite = particleengine.Utils.getLoadedSprite("graphics/fx/particlealpha_textured.png");
    private final float[] colorHSVA = new float[4];
    public float size = 25f, sizeJitter = 0.2f;
    public float life = 2f, lifeJitter = 0.25f;
    public float fadeInFrac = 0.2f, fadeOutFrac = 0.8f;
    public float randRadius = 0f;
    public Vector2f driftDirection = Misc.getUnitVectorAtDegreeAngle(MathUtils.randBetween(0f, 360f));

    public float driftSpeed = 0f;
    public float hueJitter = 5f;
    public float saturationJitter = 0.1f;
    public float alphaJitter = 0.5f;
    public float alphaMult = 1f;
    public int blendDestFac = GL11.GL_ONE;

    @Override
    public int getBlendDestinationFactor() {
        return blendDestFac;
    }

    public FireEmitter(Vector2f location) {
        this.location = location;
        setColor(new Color(255, 150, 64, 120));
    }

    public void setColor(Color newColor) {
        Utils.toHSVA(newColor.getComponents(null), colorHSVA);
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
        Vector2f offset = MathUtils.randomPointInCircle(new Vector2f(), randRadius);
        data.offset(offset);
        float newLife = MathUtils.randBetween(1f - lifeJitter, 1f + lifeJitter) * life;
        data.life(newLife);
        data.fadeTime(fadeInFrac * newLife, fadeOutFrac * newLife);
        float newSize = MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter) * size;
        data.size(newSize, newSize);
        data.growthRate(newSize / newLife, newSize / newLife);
        float[] newColorHSVA = new float[] {
                colorHSVA[0] + MathUtils.randBetween(-hueJitter, hueJitter),
                colorHSVA[1] * MathUtils.randBetween(1f - saturationJitter, 1f + saturationJitter),
                colorHSVA[2],
                colorHSVA[3] * alphaMult * MathUtils.randBetween(1f - alphaJitter, 1f + alphaJitter)
        };
        data.facing(MathUtils.randBetween(0f, 360f));
        data.turnRate(MathUtils.randBetween(-60f, 60f));
        data.colorHSVA(newColorHSVA);
        data.saturationShift(-newColorHSVA[1] / newLife);
        Vector2f newVelocity = new Vector2f(driftDirection);
        newVelocity.scale(driftSpeed);
        data.velocity(newVelocity);
        return data;
    }
}
