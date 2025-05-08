package shipmastery.fx;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import particleengine.Utils;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class StarSiphonEmitter extends BaseIEmitter {

    final PlanetAPI star;
    final Vector2f target;
    final SpriteAPI sprite;

    public StarSiphonEmitter(PlanetAPI star, Vector2f target) {
        this.star = star;
        this.target = target;
        sprite = Utils.getLoadedSprite("graphics/fx/particlealpha64sq.png");
    }

    @Override
    public Vector2f getLocation() {
        return star.getLocation();
    }

    @Override
    public float getXDir() {
        return star.getFacing()+180f;
    }

    @Override
    public float getRenderRadius() {
        return 3000f;
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        float life = MathUtils.randBetween(16f, 20f);
        data.life(life);
        data.color(new Color(1f, 1f, 1f, MathUtils.randBetween(0.05f, 0.1f)));

        float fadeScale = MathUtils.randBetween(0.5f, 1.5f);
        data.fadeTime(fadeScale * life/10f, fadeScale * life/4f);
        float scale = MathUtils.randBetween(5f, 7f);
        data.size(90f*scale, 60f*scale);
        float endScale = MathUtils.randBetween(0.5f, 0.8f);
        data.growthRate(-90f*scale/life*(1f-endScale), -60f*scale/life*(1f-endScale));

        float angle = MathUtils.randBetween(-90f, 90f);
        Vector2f vec = Misc.getUnitVectorAtDegreeAngle(angle);
        vec.scale(star.getRadius()*0.9f);
        data.offset(vec);

        Vector2f targetRel = new Vector2f(MathUtils.dist(target, star.getLocation()), 0f);
        Vector2f velVec = Vector2f.sub(targetRel, vec, null);
        Vector2f scaledVel = new Vector2f( MathUtils.randBetween(0.8f, 1f) * velVec.x / life, MathUtils.randBetween(0.8f, 1f) * velVec.y / life);
        data.velocity(scaledVel);
        data.facing((float) Math.atan2(scaledVel.y, scaledVel.x) * Misc.DEG_PER_RAD);

        return data;
    }
}
