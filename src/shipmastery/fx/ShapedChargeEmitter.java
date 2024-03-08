package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;
import particleengine.Utils;
import shipmastery.util.MathUtils;

import java.awt.Color;

public class ShapedChargeEmitter extends BaseIEmitter {

    public SpriteAPI sprite = Utils.getLoadedSprite("graphics/fx/explosion3.png");
    public float minVelocity = 50f, maxVelocity = 100f;
    public float angleSpread = 30f;
    public float angle = 0f;
    public final CombatEntityAPI source;
    public Color color = Color.WHITE;
    public float saturationShiftOverLife = 0f;
    public float life = 1f, lifeRandomness = 0f;
    public float fadeInFrac = 0.25f, fadeOutFrac = 0.25f;
    public float size = 80f, sizeRandomness = 0.25f;
    public Vector2f offset = new Vector2f();

    public ShapedChargeEmitter(CombatEntityAPI source) {
        this.source = source;
    }


    @Override
    public Vector2f getLocation() {
        return source.getLocation();
    }

    @Override
    public float getXDir() {
        return source.getFacing();
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();

        float theta = MathUtils.randBetween(angle - angleSpread / 2f, angle + angleSpread / 2f);
        float newLife = MathUtils.randBetween(life * (1f - lifeRandomness), life * (1f + lifeRandomness));
        float speed = MathUtils.randBetween(minVelocity, maxVelocity);

        data.velocity((Vector2f) Misc.getUnitVectorAtDegreeAngle(theta).scale(speed));
        data.life(newLife);
        data.offset(offset);
        data.facing(MathUtils.randBetween(0f, 360f));
        data.fadeTime(newLife * fadeInFrac, newLife * fadeOutFrac);
        data.color(color);
        float newSize = size * MathUtils.randBetween(1f - sizeRandomness, 1f + sizeRandomness);
        data.size(newSize, newSize);
        data.saturationShift(saturationShiftOverLife / newLife);
        return data;
    }
}
