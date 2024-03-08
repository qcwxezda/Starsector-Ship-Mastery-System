package shipmastery.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class ShipDisplay implements CustomUIElement {
    final ShipHullSpecAPI spec;
    final float size;

    public ShipDisplay(ShipHullSpecAPI spec, float size) {
        this.spec = spec;
        this.size = size;
    }

    @Override
    public void create(TooltipMakerAPI tooltip) {
        String spriteName = spec.getSpriteName();
        SpriteAPI sprite = Global.getSettings().getSprite(spriteName);
        float imageSize = Math.max(sprite.getWidth(), sprite.getHeight());
        imageSize = Math.min(imageSize, size - 10f);
        tooltip.setParaOrbitronVeryLarge();
        tooltip.addPara(spec.getHullName(), 0f).setAlignment(Alignment.MID);
        ButtonAPI outline = tooltip.addAreaCheckbox("", null, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                                                        Misc.getBrightPlayerColor(), size - 5f,
                                                        size - 5f, 0f);
        outline.setClickable(false);
        outline.setGlowBrightness(0.3f);
        outline.setMouseOverSound(null);
        tooltip.addImage(
                spec.getSpriteName(), size, imageSize, -imageSize - (size - imageSize) / 2f);
    }
}
