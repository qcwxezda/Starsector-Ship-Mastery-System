package shipmastery.fx;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import shipmastery.util.CollisionUtils;

import java.awt.*;
import java.util.List;

public class OutlineEmitter extends BaseIEmitter {
    ShipAPI ship;
    final SpriteAPI sprite = particleengine.Utils.getLoadedSprite("graphics/fx/particlealpha32sq.png");
    List<Vector2f> pts;
    float[] averageColor = new float[3];
    public OutlineEmitter(ShipAPI ship) {
        this.ship = ship;
        Color color = getSprite().getAverageBrightColor();
        color.getRGBColorComponents(averageColor);
    }

    @Override
    public Vector2f getLocation() {
        return ship.getLocation();
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public float getXDir() {
        return ship.getFacing();
    }

    @Override
    protected boolean preInitParticles(int start, int count) {
        pts = CollisionUtils.randomPointsOnBounds(ship, count, false);
        return true;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        int size = pts.size();
        data.offset(new Vector2f(pts.get(i % size).x - ship.getLocation().x, pts.get(i % size).y - ship.getLocation().y));
        data.life(0.1f);
        data.color(averageColor[0], averageColor[1], averageColor[2], 0.1f);
        data.size(20f, 20f);
        data.fadeTime(0f, 0.15f);
        return data;
    }
}
