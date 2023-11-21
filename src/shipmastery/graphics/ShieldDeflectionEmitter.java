package shipmastery.graphics;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.Utils;

import java.awt.*;

public class ShieldDeflectionEmitter extends BaseIEmitter {

    ShieldAPI shield;

    public ShieldDeflectionEmitter(ShipAPI ship) {
        this.shield = ship.getShield();
    }

    @Override
    public Vector2f getLocation() {
        return shield.getLocation();
    }

    @Override
    public float getXDir() {
        return shield.getFacing();
    }

    @Override
    protected boolean preInitParticles(int startingIndex) {
        return shield.getActiveArc() > 0f;
    }

    @Override
    public SpriteAPI getSprite() {
        return particleengine.Utils.getLoadedSprite("graphics/fx/particlealpha32sq.png");
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        float activeArc = shield.getActiveArc();
        float radius = Utils.randBetween(shield.getRadius() * 0.998f, shield.getRadius() * 1.002f);
        float theta = Utils.randBetween(Misc.RAD_PER_DEG * -activeArc / 2f, Misc.RAD_PER_DEG * activeArc / 2f);
        Vector2f offset = new Vector2f(radius * (float) Math.cos(theta), radius * (float) Math.sin(theta));
        data.offset(offset);
        float life = Utils.randBetween(0.6f, 0.8f);
        data.life(life);
        float size = Utils.randBetween(20f, 25f);
        data.size(size, size);
        Color shieldColor = shield.getInnerColor();
        data.color(shieldColor.getRed() / (float) 255, shieldColor.getGreen() / (float) 255, shieldColor.getBlue() / (float) 255, 0.75f);
        float velocityScale = Utils.randBetween(-0.03f, 0.03f);
        data.velocity(new Vector2f(velocityScale * offset.x , velocityScale * offset.y));
        data.growthRate(-0.25f * size / life, -0.25f * size / life);
        data.fadeTime(0f, life);
        return data;
    }
}
