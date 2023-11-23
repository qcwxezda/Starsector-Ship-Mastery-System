package shipmastery.graphics;

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
    public JitterEmitter(ShipAPI target, boolean renderAbove) {
        layer = renderAbove ? CombatEngineLayers.ABOVE_PARTICLES_LOWER : CombatEngineLayers.BELOW_SHIPS_LAYER;
        this.target = target;
        width = getSprite().getWidth();
        height = getSprite().getHeight();
        Color color = getSprite().getAverageColor();
        color.getRGBColorComponents(averageColor);
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
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.life(0.3f);
        data.fadeTime(0.1f, 0.1f);
        Vector2f offset = new Vector2f(
                MathUtils.randBetween(-30f, 30f),
                MathUtils.randBetween(-30f, 30f));
        data.offset(offset);
        data.turnRate(target.getAngularVelocity());
        data.color(averageColor[0], averageColor[1], averageColor[2], 0.7f);
        data.size(width, height);
        data.saturationShift(2f);
        data.hueShift(MathUtils.randBetween(-250f, 250f));
        return data;
    }
}
