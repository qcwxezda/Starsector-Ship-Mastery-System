package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class JitterEmitter2 extends BaseIEmitter {

    public final CombatEntityAPI entity;
    public final SpriteAPI sprite;
    public final float width, height;
    public float radius = 10f;
    public float life = 1.5f;
    public Color color = Color.WHITE;
    public float saturationShift = 0.5f;
    public float hueShift = 90f;
    public float alphaMult = 0.2f;

    public JitterEmitter2(CombatEntityAPI entity, SpriteAPI sprite) {
        this.entity = entity;
        this.sprite = sprite;
        width = sprite.getWidth();
        height = sprite.getHeight();
    }

    @Override
    public Vector2f getLocation() {
        return entity.getLocation();
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public float getXDir() {
        return entity.getFacing() - 90f;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return CombatEngineLayers.BELOW_SHIPS_LAYER;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.life(life);
        data.fadeTime(life*0.2f, life*0.8f);
        data.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, alphaMult*color.getAlpha() / 255f);
        data.offset(MathUtils.randomPointInCircle(new Vector2f(), radius));
        data.saturationShift(MathUtils.randBetween(-saturationShift, saturationShift));
        data.hueShift(MathUtils.randBetween(-hueShift, hueShift));
        data.size(sprite.getWidth(), sprite.getHeight());
        return data;
    }
}
