package shipmastery.fx;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class EntityBurstEmitter extends BurstEmitter {
    final CombatEntityAPI entity;

    public EntityBurstEmitter(CombatEntityAPI entity, SpriteAPI sprite, Color color, int numDirections, float width, float duration) {
        super(sprite, color, numDirections, width, duration);
        this.entity = entity;
    }

    @Override
    public Vector2f getLocation() {
        location.set(entity.getLocation());
        return super.getLocation();
    }

    @Override
    public float getXDir() {
        return entity.getFacing() - 90f;
    }
}
