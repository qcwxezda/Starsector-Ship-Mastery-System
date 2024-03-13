package shipmastery.combat.listeners;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public interface EMPEmitterDamageListener {
    void reportEMPEmitterHit(ShipAPI target, DamageAPI damage, Vector2f point, boolean shieldHit);
}
