package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class JitterEmitter extends BaseIEmitter {

    final CombatEngineLayers layer;
    final ShipAPI target;
    final float width, height;
    float[] averageColor = new float[3];
    int totalParticleCount;
    float alphaMult = 1f;
    float radius = 0f;

    public JitterEmitter(ShipAPI target, boolean renderAbove, int totalParticleCount) {
        layer = renderAbove ? CombatEngineLayers.ABOVE_PARTICLES_LOWER : CombatEngineLayers.BELOW_SHIPS_LAYER;
        this.target = target;
        width = getSprite().getWidth();
        height = getSprite().getHeight();
        Color color = getSprite().getAverageColor();
        color.getRGBColorComponents(averageColor);
        this.totalParticleCount = totalParticleCount;
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
        return target.getSpriteAPI();
    }

    @Override
    protected boolean preInitParticles(int start, int count) {
        float mult =  (float) Math.cos(0.5f * Math.PI / totalParticleCount * start);
        radius = 35f * mult;
        alphaMult = 0.25f + 0.75f * mult;
        return true;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.life(0.75f);
        data.fadeTime(0.25f, 0.25f);
        data.offset(MathUtils.randomPointInCircle(new Vector2f(), radius));
        data.color(averageColor[0], averageColor[1], averageColor[2], 0.3f * alphaMult);
        data.size(width, height);
        data.saturationShift(1f);
        data.hueShift(MathUtils.randBetween(-100f, 100f));
        return data;
    }
}
