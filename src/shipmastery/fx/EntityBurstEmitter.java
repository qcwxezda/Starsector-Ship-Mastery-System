package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import particleengine.BaseIEmitter;
import particleengine.ParticleData;

import java.awt.*;

public class EntityBurstEmitter extends BaseIEmitter {
    final CombatEntityAPI entity;
    final SpriteAPI sprite;
    final Color color;
    final float width;
    final float duration;
    final int numDirections;
    float alphaMult = 1f;
    float jitterRadius = 0f;
    float widthGrowth = 0f;
    CombatEngineLayers layer = CombatEngineLayers.BELOW_SHIPS_LAYER;

    public EntityBurstEmitter(CombatEntityAPI entity, SpriteAPI sprite, Color color, int numDirections, float width, float duration) {
        this.entity = entity;
        this.sprite = sprite;
        this.color = color;
        this.width = width;
        this.duration = duration;
        this.numDirections = numDirections;
    }

    @Override
    public Vector2f getLocation() {
        if (jitterRadius <= 0f) {
            return entity.getLocation();
        }
        Vector2f loc = Misc.getUnitVectorAtDegreeAngle(Misc.random.nextFloat() * 360f);
        loc.scale(Misc.random.nextFloat() * jitterRadius);
        Vector2f.add(loc, entity.getLocation(), loc);
        return loc;
    }

    @Override
    public SpriteAPI getSprite() {
        return sprite;
    }

    @Override
    public float getXDir() {
        return entity.getFacing() - 90f;
    }

    public void setAlphaMult(float alphaMult) {
        this.alphaMult = alphaMult;
    }

    public void setWidthGrowth(float amountOverLifetime) {
        this.widthGrowth = amountOverLifetime;
    }

    public void setLayer(CombatEngineLayers layer) {
        this.layer = layer;
    }

    public void setJitterRadius(float radius) {
        jitterRadius = radius;
    }

    @Override
    public CombatEngineLayers getLayer() {
        return layer;
    }

    @Override
    protected ParticleData initParticle(int i) {
        ParticleData data = new ParticleData();
        data.size(sprite.getWidth(), sprite.getHeight());
        float theta = (Misc.random.nextFloat() + i) * 360f / numDirections;
        Vector2f offset = Misc.getUnitVectorAtDegreeAngle(theta);
        Vector2f velocity = new Vector2f(offset);
        offset.scale(width);
        velocity.scale(widthGrowth / duration);
        data.offset(offset);
        data.velocity(velocity);
//        Vector2f offset = new Vector2f();
//        offset.x = i % 4 == 0 ? -width : i % 4 == 1 ? width : 0f;
//        offset.y = i % 4 == 2 ? -width : i % 4 == 3 ? width : 0f;
//        Vector2f velocity = new Vector2f();
//        velocity.x = i % 4 == 0 ? -widthGrowth / duration : i % 4 == 1 ? widthGrowth / duration : 0f;
//        velocity.y = i % 4 == 2 ? -widthGrowth / duration : i % 4 == 3 ? widthGrowth / duration : 0f;
//        data.offset(offset);
//        data.velocity(velocity);
        //data.size(sprite.getWidth() + width, sprite.getHeight() + width);
        data.life(duration);
        data.fadeTime(duration / 4f, duration / 4f);
        //data.growthRate(widthGrowth / duration, widthGrowth / duration);
        data.color(new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f * alphaMult));
        return data;
    }
}
