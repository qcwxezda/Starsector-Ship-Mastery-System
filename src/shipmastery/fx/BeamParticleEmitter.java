package shipmastery.fx;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class BeamParticleEmitter extends BaseIEmitter {

    public BeamAPI beam;
    public Color color;
    public float size = 7.5f;
    public float sizeJitter = 0.2f;
    public float maxDist = 15f;
    public float fadeInFrac = 0.2f, fadeOutFrac = 0.8f;
    public float life = 0.5f;
    public float lifeJitter = 0.5f;

    public BeamParticleEmitter(BeamAPI beam) {
        this.beam = beam;
        Color fringeColor = beam.getFringeColor();
        this.color = new Color(fringeColor.getRed() / 255f, fringeColor.getGreen() / 255f, fringeColor.getBlue() / 255f, Math.min(1f, 2f * fringeColor.getAlpha() / 255f));
    }

    @Override
    public float getRenderRadius() {
        return 9999999f;
    }

    private Vector2f from = new Vector2f(), to = new Vector2f(), perp = new Vector2f();
    @Override
    protected boolean preInitParticles(int start, int count) {
        from = beam.getFrom();
        to = beam.getTo();
        if (MathUtils.dist(from, to) <= 0f) return false;
        perp = Misc.getPerp(Misc.getUnitVector(beam.getFrom(), beam.getTo()));
        return true;
    }

    @Override
    public SpriteAPI getSprite() {
        return null;//particleengine.Utils.getLoadedSprite("graphics/fx/particlealpha32sq.png");
    }

    @Override
    protected ParticleData initParticle(int i) {
        Vector2f loc = MathUtils.randomPointOnLine(from, to);
        Vector2f offset = new Vector2f(perp);
        offset.scale(MathUtils.randBetween(-maxDist, maxDist));
        Vector2f.add(loc, offset, loc);

        ParticleData data = new ParticleData();
        data.offset(loc);
        float newLife = life * MathUtils.randBetween(1f - lifeJitter, 1f + lifeJitter);
        data.life(newLife);
        float newSize = size * MathUtils.randBetween(1f - sizeJitter, 1f + sizeJitter);
        data.size(newSize, newSize);
        data.fadeTime(newLife * fadeInFrac, newLife * fadeOutFrac);
        data.velocity(new Vector2f(-offset.x / newLife, -offset.y / newLife));
        data.color(color);
        return data;
    }
}
