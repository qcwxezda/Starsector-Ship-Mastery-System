package shipmastery.fx;

import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Misc;
import shipmastery.util.MathUtils;

import java.awt.Color;

public abstract class OverlayRenderer extends BaseCombatLayeredRenderingPlugin {

    private final ShipAPI ship;
    private final Color overlayColor;

    public OverlayRenderer(ShipAPI ship, Color overlayColor) {
        super();
        this.overlayColor = overlayColor;
        layer = CombatEngineLayers.ABOVE_PARTICLES_LOWER;
        this.ship = ship;
    }

    @Override
    public float getRenderRadius() {
        return 1000f;
    }

    @Override
    public void advance(float amount) {
        if (entity != null) {
            entity.getLocation().set(ship.getLocation());
        }
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        var origColor = ship.getSpriteAPI().getColor();
        var origBlendSrc = ship.getSpriteAPI().getBlendSrc();
        var origBlendDst = ship.getSpriteAPI().getBlendDest();
        var origAlphaMult = ship.getSpriteAPI().getAlphaMult();
        var opacity = getOpacity();
        var color = Misc.setAlpha(overlayColor, (int) (255f * MathUtils.clamp(opacity, 0f, 1f)));
        ship.getSpriteAPI().setColor(color);
        ship.getSpriteAPI().setAdditiveBlend();
        ship.getSpriteAPI().setAlphaMult(1f);
        ship.getSpriteAPI().renderAtCenter(ship.getLocation().x , ship.getLocation().y);
        ship.getSpriteAPI().setBlendFunc(origBlendSrc, origBlendDst);
        ship.getSpriteAPI().setAlphaMult(origAlphaMult);
        ship.getSpriteAPI().setColor(origColor);
    }

    @Override
    public abstract boolean isExpired();

    public abstract float getOpacity();
}
