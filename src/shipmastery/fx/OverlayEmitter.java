package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;

import java.awt.Color;

public class OverlayEmitter extends BaseIEmitter {

    final CombatEntityAPI anchor;
    final SpriteAPI sprite;
    final float life;
    public Color color = Color.WHITE;
    public CombatEngineLayers layer = CombatEngineLayers.ABOVE_PARTICLES_LOWER;

    public OverlayEmitter(CombatEntityAPI anchor, SpriteAPI sprite, float life) {
        this.anchor = anchor;
        this.sprite = sprite;
        this.life = life;
    }

    @Override
    public Vector2f getLocation() {
        return anchor.getLocation();
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public float getXDir() {
        return anchor.getFacing() - 90f;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return layer;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.size(sprite.getWidth(), sprite.getHeight());
        data.color(color);
        data.life(life);
        data.fadeTime(life / 4f, life / 4f);
        return data;
    }
}
