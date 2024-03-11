package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.opengl.GL11;
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
    public int blendDestFac = GL11.GL_ONE;
    public float alphaMult = 1f;
    public float fadeInFrac = 0.25f, fadeOutFrac = 0.25f;
    private float anchorAlpha = 1f;

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
    public int getBlendDestinationFactor() {
        return blendDestFac;
    }
    @Override
    protected boolean preInitParticles(int start, int count) {
        if (anchor instanceof ShipAPI) {
            anchorAlpha = ((ShipAPI) anchor).getSpriteAPI().getAlphaMult();
        }
        return true;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.size(sprite.getWidth(), sprite.getHeight());
        Color newColor = new Color(color.getRed()/255f, color.getGreen()/255f, color.getBlue()/255f, Math.min(1f, color.getAlpha()/255f * alphaMult * anchorAlpha));
        data.color(newColor);
        data.life(life);
        data.fadeTime(life * fadeInFrac, life * fadeOutFrac);
        return data;
    }
}
