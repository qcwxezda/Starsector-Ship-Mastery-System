package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class JitterEmitter extends BaseIEmitter {

    final CombatEngineLayers layer;
    final CombatEntityAPI target;
    final SpriteAPI sprite;
    final float width, height;
    float[] colorRGB = new float[3];
    final int totalParticleCount;
    final float peakFraction;
    final float maxRadius;
    float alphaMult = 1f;
    float radius = 0f;
    final float hueShiftDegrees;
    final float particleLife;
    float baseIntensity = 0.45f;
    float saturationShift = 0f;

    /** Peak fraction: after this fraction of particles is emitted is when the jitter offset and alpha are strongest. */
    public JitterEmitter(CombatEntityAPI target, SpriteAPI sprite, Color color, float hueShiftDegrees, float maxRadius, float particleLife, boolean renderAbove, float peakFraction, int totalParticleCount) {
        layer = renderAbove ? CombatEngineLayers.ABOVE_PARTICLES_LOWER : CombatEngineLayers.BELOW_SHIPS_LAYER;
        this.target = target;
        this.sprite = sprite;
        width = sprite.getWidth();
        height = sprite.getHeight();
        color.getRGBColorComponents(colorRGB);
        this.totalParticleCount = totalParticleCount;
        this.peakFraction = MathUtils.clamp(peakFraction, 0.001f, 1f);
        this.hueShiftDegrees = hueShiftDegrees;
        this.maxRadius = maxRadius;
        this.particleLife = particleLife;
    }

    @Override
    public Vector2f getLocation() {
        return target.getLocation();
    }

    @Override
    public float getXDir() {
        return target.getFacing() - 90f;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return layer;
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    protected boolean preInitParticles(int start, int count) {
        float r = peakFraction * totalParticleCount;
        float mult = (float) (Math.E/r * start * Math.exp(-start/r));
//        float mult =  (float) Math.cos(0.5f * Math.PI / totalParticleCount * start);
        radius = maxRadius * mult;
        alphaMult = 0.25f + 0.75f * mult;
        return true;
    }

    public void setBaseIntensity(float intensity) {
        baseIntensity = intensity;
    }

    public void setSaturationShift(float amount) {saturationShift = amount;}

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.life(particleLife);
        data.fadeTime(particleLife/2f, particleLife/2f);
        data.offset(MathUtils.randomPointInCircle(new Vector2f(), radius));
        data.color(colorRGB[0], colorRGB[1], colorRGB[2], baseIntensity * alphaMult);
        data.size(width, height);
        data.saturationShift(saturationShift);
        data.hueShift(MathUtils.randBetween(-hueShiftDegrees, hueShiftDegrees));
        return data;
    }
}
