package shipmastery.graphics;

import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.Utils;

public class FortressShieldDeflectionEmitter extends BaseIEmitter {

    ShieldAPI shield;

    public FortressShieldDeflectionEmitter(ShipAPI ship) {
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
        float radius = shield.getRadius();//Utils.randBetween(shield.getRadius() * 0.95f, shield.getRadius() * 1.05f);
        float theta = Utils.randBetween(Misc.RAD_PER_DEG * -activeArc / 2f, Misc.RAD_PER_DEG * activeArc / 2f);
        Vector2f offset = new Vector2f(radius * (float) Math.cos(theta), radius * (float) Math.sin(theta));
        data.offset(offset);
        float life = Utils.randBetween(0.4f, 0.8f);
        data.life(life);
        float size = Utils.randBetween(15f, 20f);
        data.size(size, size);
        data.color(shield.getInnerColor());
        float velocityScale = Utils.randBetween(-0.05f, 0.05f);
        data.velocity(new Vector2f(velocityScale * offset.x , velocityScale * offset.y));
        data.fadeTime(Utils.randBetween(life / 4f, life / 2f), Utils.randBetween(life / 4f, life / 2f));
        return data;
    }
}
